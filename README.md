# lein-set-version

A [Leiningen](https://github.com/technomancy/leiningen) plugin to update the
project version.

## Usage

Put `[lein-set-version "0.3.0"]` into the `:plugins` vector of your `:user`
profile.

For example, to update the version of your project to 1.0.0

    $ lein set-version 1.0.0

This will also work with `lein-sub`, and will update the intra-project
dependency versions too.

Without a version argument, and "-SNAPSHOT" is removed from the current project
version and that version is used.

## Updating other files

Apart from `project.clj`, lein-set-version can also update other files. To
configure this, add entries to the `:updates` vector in the project
`:set-version` key. Each entry should be a map, containing at least a `:path`
key with the project relative path of the file to be updated.

```clj
:set-version
 {:updates [{:path "README.md" :no-snapshot true}
            {:path "src/leiningen/ritz.clj"
             :search-regex
             #"some text \"\d+\.\d+\.\d+(-SNAPSHOT)?\""}]}
```

`:no-snapshot`
: Prevents updating the file to a "-SNAPSHOT" version. Defaults to false.

`:search-regex`
: Finds text to be replaced. Defaults to a regex for the existing project
  version (or the previous version if the current version is a snapshot and
  `:no-snapshot` is true).

`:replace-regex`
: A regex applied to the text matched by `:search-regex`. The matching region is
  replaced with the new version. Defaults to a regex for the existing project
  version (or the previous version if the current version is a snapshot and
  `:no-snapshot` is true).

Note that the previous version may need to be supplied on the command line when
`:no-snapshot` is used, and the current version ends in a ".0". You can specify
this using the `:previous-version` keyword.

    lein set-version 1.1.0 :previous-version 1.0.2

## Dry Run Mode

You can pass `:dry-run true` to run without changing any files.  In this mode
the changes to be made are displayed.

## See also

[lein-release](https://github.com/relaynetwork/lein-release)
: automatically manages your project’s version and deploys the built artifact

[lein-sub](https://github.com/kumarshantanu/lein-sub)
: execute tasks on sub-projects

[Leiningen Plugins Wiki](https://github.com/technomancy/leiningen/wiki/Plugins)
: comprehensive list of plugins

## License

Copyright © 2012 Hugo Duncan

Distributed under the Eclipse Public License.
