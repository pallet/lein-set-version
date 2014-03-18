#!/bin/bash

# release lein-set-version

if [[ $# -lt 2 ]]; then
  echo "usage: $(basename $0) previous-version new-version" >&2
  exit 1
fi

previous_version=$1
version=$2

echo ""
echo "Start release of $version, previous version is $previous_version"
echo ""
echo ""

lein do clean, test && \
git flow release start $version || exit 1


function log-without () {
    export previous_version
    git --no-pager log --format=%H $previous_version.. |
    grep -v -f <(git --no-pager log --format=%H --grep=Merge --grep=travis --grep='Updated version for next' --grep='Updated project.clj, release notes and readme for' --grep='release script' --grep='Update vesion for next' --grep='Update project version' $previous_version.. ) |
    git log --pretty=changelog --stdin --no-walk
}


lein with-profile release set-version ${version} :previous-version ${previous_version} \
  || { echo "set version failed" >2 ; exit 1; }

echo ""
echo ""
echo "Changes since $previous_version"
tmpfile=$(mktemp releaseXXXXX)
printf "## ${version}\n\n$(log-without)\n\n" | cat - ReleaseNotes.md > $tmpfile && mv -f $tmpfile ReleaseNotes.md || exit 1
echo ""
echo ""
echo "Now edit project.clj, ReleaseNotes and README"

$EDITOR project.clj
$EDITOR ReleaseNotes.md
$EDITOR README.md

echo -n "commiting project.clj, release notes and readme.  enter to continue:" \
&& read x \
&& git add project.clj ReleaseNotes.md README.md \
&& git commit -m "Updated project.clj, release notes and readme for $version" \
&& echo -n "Peform release.  enter to continue:" && read x \
&& lein do clean, test \
&& git push origin $(git rev-parse --abbrev-ref HEAD) \
&& echo -n "Wait for travis to push to master" \
&& read x \
&& git checkout master \
&& git pull \
&& lein deploy clojars
