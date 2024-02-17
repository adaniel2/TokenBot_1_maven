
# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [#####] - Unreleased

### Added

### Fixed

### Changed

### Removed


## [1.1.1] - 17/02/2024

### Added

### Fixed
- isCurator() function bug
- "balance" command bug

### Changed

### Removed


## [1.1.0] - 17/02/2024

### Added
- New table in database to track submissions
- Utility functions to read/write submissions to database
- Automated process of marking reviewed submissions with a checkmark on discord
- Automated process of announcing latest review session results
- New command "reviewSubs" to trigger processing + announcement to users who submitted during current batch

### Fixed

### Changed
- Improved method for duplicate checking
- Improved messages sent to users
- Reserving variable 'e' for error catching
- Taking advantage of singleton and not passing SpotifyAPI in constructors anymore
- Removed the need for Discord API imports in SpotifyAPI class
- isCurate() check now also accepts curator list (might switch to just using discord roles in the future)
- Playlist IDs in SpotifyAPI are now static
- No longer using custom checkmark for reactions

### Removed


## [1.0.3] - 30/08/2023

### Changed

- Removed embeds from message sent by bot confirming submission


## [1.0.2] - 28/08/2023

### Fixed

- Fixed a typo in a logger message

### Changed

- Committing fully to "TokenBot" as app name
- Improved successful submission message


## [1.0.1] - 26/08/2023

### Added

- Message is now sent to user when an invalid URL is submitted
- Duplicate checking: If a submission is already queued for review, discard duplicates and notify user

### Changed

- Renamed "info channel" to "help channel" and adjusted accordingly
- Moved custom Exceptions to a dedicated folder


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

[unreleased]: https://github.com/adaniel2/TokenBot_1_maven/compare/1.1.1...HEAD
[1.1.1]: https://github.com/adaniel2/TokenBot_1_maven/compare/1.1.0...1.1.1
[1.1.0]: https://github.com/adaniel2/TokenBot_1_maven/compare/1.0.3...1.1.0
[1.0.3]: https://github.com/adaniel2/TokenBot_1_maven/compare/1.0.2...1.0.3
[1.0.2]: https://github.com/adaniel2/TokenBot_1_maven/compare/1.0.1...1.0.2
[1.0.1]: https://github.com/adaniel2/TokenBot_1_maven/compare/1.0.0...1.0.1
[1.0.0]: https://github.com/adaniel2/TokenBot_1_maven/releases/tag/1.0.0