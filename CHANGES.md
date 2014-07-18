# Grapi Releases #

### Version 0.4.4-SNAPSHOT ###

This release includes:

* [Enhancement #6](https://github.com/kalixia/Grapi/issues/6) Support @FormParam parameters extraction
* [Enhancement #7](https://github.com/kalixia/Grapi/issues/7) Support @QueryParam parameters extraction
* [Enhancement #9](https://github.com/kalixia/Grapi/issues/9) Support @HeaderParam parameters extraction
* [Enhancement #10](https://github.com/kalixia/Grapi/issues/10) Support @CookieParam parameters extraction
* [Enhancement #8](https://github.com/kalixia/Grapi/issues/8) Improve parameters conversion

### Version 0.4.3 ###

This release includes:

* [Bug #5](https://github.com/kalixia/Grapi/issues/5) IllegalArgumentException if X-Api-Request-ID is not a valid UUID

### Version 0.4.2 ###

This release includes:

* [Enhancement #4](https://github.com/kalixia/Grapi/issues/4) Scan for Shiro annotations and ensure the security constraints are met
* [Enhancement #2](https://github.com/kalixia/Grapi/issues/2) Replaced Grapi CORSCodec with new Netty ``` CorsHandler ```

### Version 0.4.1 ###

This release fixes some bugs.

* [Bug #1](https://github.com/kalixia/Grapi/issues/1) Fixed bug with CORSCodec when using preflight requests with Chrome

### Version 0.4  ###

This release includes:

* fix for APT processor introducing issues with Dagger one,
* updates to many dependencies (Netty, RxJava, etc.),
* many code quality fixes
