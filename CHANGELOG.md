
# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.0.1] - 26/08/2023

### Added

- Message is now sent to user when an invalid URL is submitted
- Duplicate checking: If a submission is already queued for review, discard duplicates and notify user

### Fixed



### Changed

- Renamed "info channel" to "help channel" and adjusted accordingly
- Moved custom Exceptions to a dedicated folder

### Removed



## [1.0.0] - 26/08/2023

### Added

- Added Curator and CuratorList classes
- Can now distinguish between URI and URL
- hasToken and removeToken re-introduced, with the ability to disable token requirement
- Added method to help read curators.json from database

### Fixed

- Many improvements to CommentWatcher logic and submission handling

### Changed

- .gitignore updated
- Url validator dependency added to pom.xml
- Cleaned up folder structure
- Balance command will use == instead of .contains(), I think it will avoid grabbing similar named tokens
- Updated Spotify regex in CommentWatcher
- There is now an admin for authentication, and then there are curators (who are still kind of admins)
- Allowing leading/trailing text with submissions

### Removed

- railway.json removed (will use web UI)
- Decided to get rid of the submission review process code which was commented out; no longer taking that route

[unreleased]: 
[1.0.0]: 