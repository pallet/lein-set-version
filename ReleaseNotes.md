## 0.4.0

- Add keyword arguments for version
  The :point, :minor and :major keywords can now be used to specify which
  part of the version number should be stepped.

  The default version can now infer a snapshot version.

## 0.3.0

- Add a dry-run mode
  In dry-run mode, no files are changed, but chnages to be made are
  displayed.

## 0.2.1

- Fix update-file-version for case of multiple matches in file
  When the search matched more than once there was an infinite loop.

  Fixes #2


## 0.2.0

- Allow update of files in addition to project.clj
  When releasing, it is common for versions to need updating in README
  files, etc. This lets the project configure a list of files to update.

  Closes #1

## 0.1.1

- Fix project file locations when running with lein sub

## 0.1.0

- Initial version
