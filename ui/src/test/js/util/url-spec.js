/* jslint node: true */
/* global describe, it, expect */

"use strict";

var url = require('../helper').require('util/url');

describe("util/url-spec", function () {
    it("- test_trimLeadingSlashes", function () {
        expect(url.trimLeadingSlashes('a')).toBe('a');
        expect(url.trimLeadingSlashes('a/')).toBe('a/');
        expect(url.trimLeadingSlashes('/a/')).toBe('a/');
        expect(url.trimLeadingSlashes('//a/')).toBe('a/');
    });

    it("- test_trimTrailingSlashes", function () {
        expect(url.trimTrailingSlashes('a')).toBe('a');
        expect(url.trimTrailingSlashes('/a')).toBe('/a');
        expect(url.trimTrailingSlashes('/a/')).toBe('/a');
        expect(url.trimTrailingSlashes('/a//')).toBe('/a');
    });

    it("- test_trimSlashes", function () {
        expect(url.trimSlashes('a')).toBe('a');
        expect(url.trimSlashes('//a')).toBe('a');
        expect(url.trimSlashes('a//')).toBe('a');
        expect(url.trimSlashes('//a//')).toBe('a');
    });

    it("- test_concatPathTokens_string", function () {
        expect(url.concatPathTokens('a')).toBe('a');
    });

    it("- test_concatPathTokens_array", function () {
        expect(url.concatPathTokens(['a', 'b'])).toBe('a/b');
        expect(url.concatPathTokens(['a/', 'b'])).toBe('a/b');
        expect(url.concatPathTokens(['a//', '///b'])).toBe('a/b');
        expect(url.concatPathTokens(['a//', '///b', '/c'])).toBe('a/b/c');
        expect(url.concatPathTokens(['a//', '///b//', '/c'])).toBe('a/b/c');
        expect(url.concatPathTokens(['a', 'b', 'c'])).toBe('a/b/c');

        // should only trim excess slashes between tokens
        // i.e. should not trim leading slashes on 1st and trailing slashes on last
        expect(url.concatPathTokens(['/a', 'b', 'c/'])).toBe('/a/b/c/');
    });
});
