# lein-set-version

A Leiningen plugin to update the project version.

## Usage

Put `[lein-set-version "0.1.0-SNAPSHOT"]` into the `:plugins` vector of your
`:user` profile.

For example, to update the version of your project to 1.0.0

    $ lein set-version 1.0.0

This will also work with lein-sub, and will update the intra-project dependency
versions too.

Without a version argument, and "-SNAPSHOT" is removed from the current project
version and that version is used.

## See also

[lein-release](https://github.com/relaynetwork/lein-release)
: automatically manages your project’s version and deploys the built artifact

## License

Copyright © 2012 Hugo Duncan

Distributed under the Eclipse Public License.
