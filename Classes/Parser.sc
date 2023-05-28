/*
[general]
title = "Parser"
summary = "a base class for Parsers"
categories = "Parsing Tools"
related = "Classes/ParserState, Classes/StrParser, Classes/RegexParser, Classes/Choice, Classes/SequenceOf, Classes/Many, Classes/ManyOne, Classes/ParserFactory"
description = '''
Parser is the base class for everything that performs parsing in this library.
Parsers for complex systems are made by combining together Parsers for smaller systems
using special-purpose parsers like Choice and SequenceOf.

One can call map on a Parser to get a new Parser that manipulates the parsing result in some way.
E.g. in an IntegerParser, it would make sense to add a map that transforms the result into an actual integer as opposed to a just a string.

One can call chain on a Parser to decide how parsing will proceed based on the parsing results so far. See the unit tests for some example.

One can call errorMap on a Parser to contextualize a parsing error message (i.e. replace the generic error message with something that makes sense to the end user - in one case an IntegerParser could represent a house number whereas in another case it could represent a number of apples. Using errorMap you can warn that the system failed to parse a "house number" as opposed to warning that it failed to parse an "integer").
'''
*/
Parser {
	/*
	[method.parserStateTransformer]
	description = '''
	a function that takes a ParserState and returns an updated ParserState
	this function forms the heart of every Parser
	'''
	[method.parserStateTransformer.returns]
	what = "ParserState"
	*/
	var <>parserStateTransformer;

	/*
	[classmethod.new]
	description = "creates a new Parser"
	[classmethod.new.returns]
	what = "Parser"
	*/
	*new {
		^super.new.parserInit;
	}

	/*
	[method.parserInit]
	description = "initializes a new Parser"
	[method.parserInit.returns]
	what = "Parser"
	*/
	parserInit {
		this.parserStateTransformer = nil;
	}

	/*
	[method.run]
	description = "runs the Parser on a piece of text and produces a ParserState. The ParserState contains the parse result or error information. Running the parser happens by calling its parserStateTransformer function on an initial state constructed from the targetString."
	[method.run.args]
	targetString = "text to be parsed"
	[method.run.returns]
	what = "ParserState"
	*/
	run {
		| targetString |
		var initialState = ParserState(
			targetString: targetString,
			index: 0,
			result: nil,
			isError: false,
			errorMsg: "");
		^this.parserStateTransformer.(initialState);
	}

	/*
	[method.map]
	description = '''
	map takes a Parser an produces a new Parser that manipulates its parsing result according to the wishes of the user.

	map allows an arbitrary transformation on a parser result to be specified
	fn is the function that maps the parse result to the desired result

	e.g. you could use map to transform the result of an IntegerParser from a string to an actual integer
	'''
	[method.map.args]
	fn = "a user-provided function that transforms the string parse result into the desired parse result (e.g. conversion from string to integer)"
	[method.map.returns]
	what = "a new Parser that will transform the parse result using the fn function"
	*/
	map {
		| fn |
		var newP = Parser();
		newP.parserStateTransformer = {
			| parserState |
			var nextState = this.parserStateTransformer.(parserState);
			if (nextState.isError) {
				nextState;
			} {
				nextState.updateResult(fn.(nextState.result));
			}
		};
		^newP;
	}

	/*
	[method.chain]
	description = '''
	several ways to explain the same thing:

	fn takes parse result and returns a parser
	chain returns new state after running parser selected by fn on current state

	chain takes a Parser and produces a new Parser. This new parser is made according to a user-specified function fn,
	which will receive the current parser state as an argument, and therefore can take it into account (i.e. the user supplied function can inspect the parse results until now to decide how to proceed). This makes the parsing process context-aware.

	chain allows dynamic selection of a next parser based on what was just parsed
	fn is the function that selects the next parser based on the parse result

	chain takes a function that takes a parse result, and uses it to select a next parser,
	and runs that next parser on the state so far to return the next state

	because map and chain take Parsers and produce new Parsers, they can also be combined in any order you like
	'''
	[method.chain.args]
	fn = "user supplied function that takes parse result so far and returns a new Parser based on that result"
	[method.chain.returns]
	what = "Parser"
	*/
	chain {
		| fn |
		var newP = Parser();
		newP.parserStateTransformer = {
			| parserState |
			var nextState;
			nextState = this.parserStateTransformer.(parserState);
			if (nextState.isError)
			{
				nextState;
			} {
				var nextParser = fn.(nextState.result);
				nextParser.parserStateTransformer.(nextState);
			};
		};
		^newP;
	}

	/*
	[method.errorMap]
	description ='''
	errorMap takes a Parser and produces a new Parser with a modified error message. This allows you to contextualize the  error messages (e.g. warn about a "missing house number" instead of a generic "failed to parse integer".)
	[method.errorMap.args]
	fn = "user supplied function that receives generic error msg and index in the targetString at the moment parsing goes wrong, and returns a new error msg"
	[method.errorMap.returns]
	what = "Parser"
	'''
	*/
	errorMap {
		| fn |
		var newP = Parser();
		newP.parserStateTransformer = {
			| parserState |
			var nextState = this.parserStateTransformer.(parserState);
			if (nextState.isError.not) {
				nextState;
			} {
				nextState.updateError(fn.(nextState.errorMsg, nextState.index));
			}
		};
		^newP;
	}
}