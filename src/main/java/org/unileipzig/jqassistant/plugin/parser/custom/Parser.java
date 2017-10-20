package org.unileipzig.jqassistant.plugin.parser.custom;

/**
 * Implementation of a Parsing Strategy outlined in [Pratt, 1973]
 *  "Top down operator precedence" https://doi.org/10.1145/512927.512931
 * Other Articles about that Parsing Strategy:
 *  - http://javascript.crockford.com/tdop/tdop.html
 *  - http://effbot.org/zone/simple-top-down-parsing.htm
 *  Discussion about combining Pratt with other Parsing Strategies, such as PEGParser/Packrat:
 *  - https://news.ycombinator.com/item?id=10731002
 */
public interface Parser {
    public Object parse(String input) throws Exception;
}
