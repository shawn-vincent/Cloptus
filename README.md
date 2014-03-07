# Cloptus v1.0

Cloptus is a Java library for parsing command-line arguments.

Parses command-line arguments for programs using a restricted GNU-getopt syntax. In particular, Cloptus supports the following argument syntaxes:
```
        --longName [arg]  (long option name)
        --longName=[arg]  (long option name, using '=' to specify argument)
        -A [arg]          (short, single character, option name)
        -B [arg]          
        -AB [arg] [arg]   (list of short options merged together)
        [arg]             (positional arguments) 
```

*Limitations:* Cloptus does *not* support syntaxes wherein the argument is concatenated with the option name (i.e. -Aarg), or optional arguments. Both of these features cause parse ambiguity, which is tricky. I may add support for this in the future.

Cloptus has a very simple, powerful, and flexible API that supports several styles of working with arguments.

Cloptus has a very powerful and extensible option type hierarchy. Powerful types that support Files, URIs, as well as most normal primitive types are provided. It is very easy to add new ones.

## News

### January 16th/2007 - public release

Cloptus has been around for a long time, but I've never actually released it until now. Cloptus is a complete rewrite of Clapi, my previous command line API, and is much improved: cleaner, more powerful, better errors, more extensible. Better software all around.

## Prerequisites

Cloptus is a JDK1.5 library: it uses generics and annotations.

## Obtaining Cloptus

Cloptus is a single Java source file, which can be downloaded here. It was deliberately kept contained, so that it would be easy to insert into arbitrary software projects.

https://github.com/shawn-vincent/Cloptus

## Documentation

All documentation for Cloptus is in the source code. You can build JavaDocs from Cloptus.java, and it should be pretty good.

## License

Cloptus is copyright &copy;2007 by me, Shawn Vincent. Cloptus is hereby placed in the public domain. I'd be thrilled to hear about it if you use it, but it is by no means necessary.

I will gladly accept additions to Cloptus. If you have a fun extension, and you're getting bored of re-patching the source every time a new version comes out, send me your patch, and I'll incorporate it. These patches, of course, will be placed under the same license as the rest of the product. Credit to the author will be inserted in the source file.

## In Summary

I hope you enjoy Cloptus. If you have any comments or requests for enhancements, mail me at svincent@svincent.com, and I'll see what I can do. Bugs, of course, will be fixed as soon as possible.

