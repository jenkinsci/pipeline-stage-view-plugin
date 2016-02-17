/*
 * The MIT License
 *
 * Copyright (c) 2013-2016, CloudBees, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

exports.concatPathTokens = function (tokens) {
    if (typeof tokens === 'string') {
        return tokens;
    } else {
        var concatedString = '';
        for (var index = 0; index < tokens.length; index++) {
            if (index === 0) {
                concatedString += exports.trimTrailingSlashes(tokens[index]);
            } else if (index === tokens.length - 1) {
                concatedString += '/' + exports.trimLeadingSlashes(tokens[index]);
            } else {
                concatedString += '/' + exports.trimSlashes(tokens[index]);
            }
        }
        return concatedString;
    }
}

exports.trimLeadingSlashes = function (string) {
    return string.replace(/^\/+/g, '');
}
exports.trimTrailingSlashes = function (string) {
    return string.replace(/\/+$/g, '');
}
exports.trimSlashes = function (string) {
    return exports.trimLeadingSlashes(exports.trimTrailingSlashes(string));
}
