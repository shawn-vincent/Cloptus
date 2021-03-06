/*
 * Cloptus.java
 *
 * Copyright (c) 2007 Shawn Vincent.  All Rights Reserved.
 * Hereby donated to the public domain.
 *
 * INVESTIGATE: needs work, figure out what needs doing and do it
 * (there's nothing very hard left)
 *
 * A new version of the Command line API class.
 *
 * svincent@svincent.com
 * http://www.svincent.com/
 *
 * NOTE: now supports array OptTargets, but not collection ones.
 *
 * Simple copy-n-paste usage:

... import ...

import com.svincent.util.Cloptus.*;

... variant 1 (define your @OptTargets, and then...) ...

  if (Opts.run (target, args) == null) return;

... variant 2 ...

  OptSet opts = new OptSet ();
  FileOpt inOpt = new FileOpt (opts, "in")
    .positional (true);
  ParsedArgs parsedArgs = opts.run (args);
  if (parsedArgs == null) return;
  File in = inOpt.get (parsedArgs);

 *
 * ChangeLog:
 *
 *   Jan 19/2005 - fixed default value handling, also fixed a bug in
 *   option parsing that let an argument "-" through without an error,
 *   thinking it was an empty set of short opts. Also added
 *   getStringContent() and getByteContent() for FileOpt and
 *   UriOpt. (spv)
 *
 * TODO (required):
 *
 * - add a PropertyOpt (name/value pair)
 * - make Collection/array @OptTargets work
 * - make enum parsing case-insensitive
 * - finish scouring the documentation
 * - test, test, test
 *
 * TODO (contentious):
 *
 * - make field sets/setter calls fire in order based on command line
 *   arguments.  Do default values at the end.
 *   BUT: what about list options??
 *
 * TODO (nice to have):
 *
 * - remove dependence on Date.parse()
 * - make @OptTarget compatible with convenience setters
 * - support SI units (k, M, etc) for numeric options
 * - add additional Opt<A> subtypes for fun stuff
 * - improve date parsing to support a broader range of dates.
 */
package com.svincent.util;

import java.io.*;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.net.*;
import java.util.*;

/**
 * <p><b>Start Here:</b> A complete, robust, and pleasant command line
 * option parsing API.
 *
 * <h3>Getting Started</h3>
 *
 * <p>To quickly get started, see the {@link Cloptus.OptTarget}
 * annotation documentation.  This is the easiest way to make options
 * work.
 *
 * <p>If you want to do more sophisticated stuff, see {@link
 * Cloptus.OptSet} and {@link Cloptus.Opt} and its subtypes.
 *
 * <h3>Overview</h3>
 *
 * <p>Parses command-line arguments for programs using a restricted
 * GNU-getopt syntax.  In particular, Cloptus supports the following
 * argument syntaxes:
 *
 * <pre class="code">
 *     --longName [arg]  (long option name)
 *     --longName=[arg]  (long option name, using '=' to specify argument)
 *     -A [arg]          (short, single character, option name)
 *     -B [arg]          
 *     -AB [arg] [arg]   (list of short options merged together)
 *     [arg]             (positional arguments) </pre>
 *
 * <p><b>Limitations:</b> Cloptus does <b>not</b> support syntaxes
 * wherein the argument is concatenated with the option name
 * (i.e. <tt>-Aarg</tt>), or optional arguments.  Both of these
 * features cause parse ambiguity, which is tricky.  I may add support
 * for this in the future.
 *
 * <p>Cloptus has a very simple, powerful, and flexible API that
 * supports several styles of working with arguments.
 *
 * <p>Cloptus has a very powerful and extensible option type
 * hierarchy.  Powerful types that support Files, URIs, as well as
 * most normal primitive types are provided.  It is very easy to add
 * new ones.
 *
 * @see Cloptus.OptTarget
 * @see Cloptus.OptSet
 * @see Cloptus.Opt
 **/
public class Cloptus {

  public static final boolean DebugParse = false;
  public static final boolean DebugValidate = false;

  /**
   * Do not construct instances of Cloptus.
   **/
  private Cloptus () {}

  // -------------------------------------------------------------------------
  // ---- Demos --------------------------------------------------------------
  // -------------------------------------------------------------------------

  /**
   * An example enum for use in the demos.
   **/
  static enum color { Blue, Red, Green };

  /**
   * <p>A demo of the Cloptus's @OptTarget mechanism.  Using this
   * mechanism, your option parsing can be done with a couple of
   * annotations and two lines of code.
   *
   * <p>Parsing options has never been so easy!
   *
   * <p>Try it out!
   *
   * <pre class="code">
   * java com.svincent.util.Cloptus$AnnotationDemo --help
   * </pre>
   *
   * <p>(You may need to \-quote the $ if you're running under a UNIX
   * shell)
   **/
  public static class AnnotationDemo {

    /**
     * <p>The simplest use of @OptTarget.
     *
     * <p>If you have a public field, just stick the @OptTarget
     * annotation on it.  It will automatically get an annotation
     * corresponding to the option.
     *
     * <p>In this case, this is equivalent to:
     * <pre class="code">new FileOpt (opts, "file");</pre>
     *
     * <p>(Of course, using the @OptTarget annotation means that
     * Cloptus will also set the value of the field for us...)
     *
     * @see Cloptus.OptTarget
     **/
    @OptTarget
    public File file;

    /**
     * <p>This is a private member.  Thus, you can't put an @OptTarget
     * on it.
     *
     * <p>In fact, it would be creepy if you could: Cloptus should not
     * be able to see, let alone modify, the private members of other
     * classes.
     *
     * <p>Use a setter instead.
     *
     * @see #setAge(int)
     **/
    private int age;

    /**
     * An example of a list option: doesn't work yet.
     **/
    @OptTarget
    public File[] inputFile;

    /**
     * <p>Because 'age' is a private member, we need to specify @OptTarget
     * on a setter instead.
     *
     * <p>Cloptus will allow you to use any method with one parameter
     * as a setter.  I'll leave the devious possibilities of option
     * callbacks as an excercise for the reader.
     *
     * <p>Here we're also demonstrating the use of one of the optional
     * parameters to @OptTarget: description.  This means that the
     * --help text for the generated option will have a description.
     * Much fun.
     *
     * <p>This is equivalent to:
     *
     * <pre class="code">new IntegerOpt (opts, "age").description ("The age of the creature")</pre>
     *
     * @see Cloptus.OptTarget
     **/
    @OptTarget (description="The age of the creature")
    public void setAge (int v) { age = v; }

    /**
     * A simple constructor: doesn't do anything at all.
     **/
    public AnnotationDemo ()
    {}

    /**
     * <p>Here we see the power of annotations.  With a single call to
     * {@link OptSet#populateMembers(Object,String[])}, we get:
     *
     * <ul>
     *
     *   <li>option definition based on all of our @OptTarget
     *   annotations.
     *
     *   <li>fancy help processing, error handling, the works!
     *
     *   <li>Cloptus conveniently filling out all of our member variables
     *       based on our @OptTarget specifications.
     *
     * </ul>
     *
     * <p>What could be easier than that?
     *
     * <pre class="code">
     * AnnotationDemo test = new AnnotationDemo ();
     * if (OptSet.populateMembers (test, args) == null) return;
     * System.out.println (test);
     * </pre>
     *
     * @see Cloptus.OptTarget
     * @see Cloptus.OptSet#populateMembers(Object,String[])
     **/
    public static void main (String[] args)
    {
      // --- convenience API: it's so easy, it's hard to justify
      //   - commenting it at all.
      AnnotationDemo test = new AnnotationDemo ();
      if (OptSet.populateMembers (test, args) == null) return;
      System.out.println (test);
    }

    /**
     * <p>This is a demonstration of fancier things you can do with
     * the annotations API.
     *
     * <p>The {@link Cloptus.OptSet#populateMembers(Object,String[])}
     * method hides this code pattern.  If you use the pattern
     * directly, you can do fancy things like add additional
     * arguments, process the result ParsedArgs, and even edit the
     * autogenerated OptSet directly.
     *
     * @see Cloptus.OptTarget
     * @see Cloptus.OptSet#populateMembers(Object,String[])
     **/
    public static void main_advanced (String[] args)
    {
      // --- full API (this is what the code in main() expands into)

      // --- make a target object.
      AnnotationDemo test = new AnnotationDemo ();

      // --- build a new OptSet
      OptSet opts = new OptSet ();

      // --- define the autogenerated @OptTarget opts for the given type.
      opts.defineOptsForType (test.getClass ());

      // --- run the option parser, doing error/help handling.
      ParsedArgs parsedArgs = opts.run (args);
      if (parsedArgs == null) return;

      // --- populate the members in the target object
      parsedArgs.populateMembers (test);

      // --- print the result: prove it worked.
      System.out.println (test);
    }

    /**
     * <p>Simple toString behavior to demonstrate that the
     * command-line arguments get fed into fields.
     **/
    public String toString ()
    { return "AnnotationDemo(file="+file+", age="+age+")"; }
  }

  /**
   * <p>Sample code demonstrating the traditional (and most powerful)
   * usage pattern of Cloptus.
   **/
  @SuppressWarnings("unused")
public static void main (String... args)
  {
    // ---
    // --- Specifying options: easy as pie!
    // ---

    // --- build an OptSet object to contain the options.
    OptSet opts = new OptSet ();

    // --- StringOpt: generic, used for String parameters.
    StringOpt stringOpt =
      new StringOpt (opts, "mystring")
      .description ("A friendly String, for tying packages");

    // --- IntegerOpt: for numeric values.
    IntegerOpt intOpt =
      new IntegerOpt (opts, "myint").defaultValue (23)
      .description ("A friendly integer.  Note that this opt supports "
                    +"0x... (hex), 0... (octal) and 0b... (binary) "
                    +"syntaxes.  If I ever get my act together, this will "
                    +"also support metric units (like K, M, etc) "
                    +"(default 23)");

    // --- DoubleOpt: for completeness, mostly.
    DoubleOpt doubleOpt =
      new DoubleOpt (opts, "mydouble");

    // --- BooleanOpt: of the form --arg=true.  If you want a flag,
    // --- use FlagOpt.
    BooleanOpt booleanOpt =
      new BooleanOpt (opts, "myboolean")
      .description ("A nice boolean arg: this one takes "
                    +"an argument (FlagOpt does not)");

    // --- A non-argumented boolean option.
    FlagOpt flagOpt =
      new FlagOpt (opts, "myflag");

    // --- A date option
    DateOpt dateOpt =
      new DateOpt (opts, "mydate");

    // --- An enum opt: cute one, will only allow values from the
    // --- specified Enum.  Should fix to allow case-insensitive
    // --- matching of enum names.  Currently case sensitive.
    EnumOpt<color> colorOpt =
      new EnumOpt<color> (opts, "mycolor", color.class)
      .description ("color flag");

    // --- Specifies a File, and then provides a host of useful
    // --- methods to fetch the file nicely.
    // --- Automatically normalizes to platform-specific slash format,
    // --- among other nice behavior.
    FileOpt fileOpt =
      new FileOpt (opts, "myfile").shortName ('f').positional (true)
      .description ("A FILE to process.  This description will go on "
                    +"and on.  This is getting quite ridiculous.  "
                    +"Really.  Some people are kindof wierd. And "
                    +"here is some more.")
      .list (true);

    // --- specifies an URI, but will also accept platform-specific
    // --- filenames and convert them into file: URIs automatically.
    // --- Also provides useful methods to fetch the contents of the
    // --- URI, or fetch the URI as an URL, a String, whatever you
    // --- want.
    UriOpt uriOpt =
      new UriOpt (opts, "myuri")
      .description ("An URI: check this out.");

    // ---
    // --- Parsing arguments is straightforward, too!
    // ---

    // --- Parse arguments: You can call 'parse' and handle the errors
    // --- yourself, but this is easier.  Handles help, error
    // --- printing.  Returns null if program should exit.
    ParsedArgs parsedArgs = opts.run (args);
    if (parsedArgs == null) return;

    // ---
    // --- Using arguments: you've got lots of options here.
    // ---

    // --- you can fetch arguments using various typed convenience
    // --- APIs on the arguments themselves.
    System.out.println ("myfile == "+fileOpt.getList (parsedArgs));
    System.out.println ("myint == "+intOpt.getInt (parsedArgs));

    // --- you can fetch options using convenient, typesafe, APIs
    System.out.println ("mystring == "
                        +opts.getByLongName ("mystring", StringOpt.class)
                        .get (parsedArgs));

    // --- you can iterate through the parsed options, in the order
    // --- they were specified.
    System.out.println ("Parsed arguments, in order:");
    for (ParsedArg parsedArg : parsedArgs)
      System.out.println ("  " + parsedArg);

//     // --- you can iterate through all the arguments, including
//     // --- synthesized arguments for defaulted values.
//     System.out.println ("Positional options, in order:");
//     for (ParsedArg parsedArg : parsedArgs.getParsedArgsWithDefaults ())
//       System.out.println ("  " + parsedArg);
  }

  /**
   * <p>A tiny implementation of wget, based mostly on URI options.
   *
   * <p>Demonstrates how much fun the convenience getters are.
   **/
  public static void wget (String... args)
    throws MalformedURLException, IOException
  {
    // --- build options
    OptSet opts = new OptSet ();
    UriOpt uriOpt = new UriOpt (opts, "uri").positional (true).required (true);

    // --- parse options
    ParsedArgs parsedArgs = opts.run (args);
    if (parsedArgs == null) return;

    // --- dump website
    System.out.println (uriOpt.getStringContent (parsedArgs));
  }

  // -------------------------------------------------------------------------
  // -------------------------------------------------------------------------
  // -------------------------------------------------------------------------

  /**
   * <p>Specifies that the annotated field or method be the target for
   * a command-line option.
   *
   * <p>Specifying @OptTarget on a field or method, and calling {@link
   * OptSet#populateMembers(Object,String[])} does two things:
   *
   * <ul>
   *    <li>An appropriate Opt subtype is automatically created for
   *    the field or method.
   *
   *    <li>The field or method is automatically populated with the
   *    result of parsing the command line arguments for that option.
   * </ul>
   *
   * <p>This makes specification of command line arguments very easy.
   *
   * <p>For example, if you want a command line argument to be fed
   * into an integer field, you can just do this:
   *
   * <pre class="code">@OptTarget public int myVar;</pre>
   *
   * <p>This automatically creates the command-line option --myVar
   * that will do all the work for you.
   *
   * <p>Note that this only works on public fields.  If you want to
   * make your fields private, just use a setter instead.
   *
   * <pre class="code">@OptTarget public void setMyVar (int v) {...}</pre>
   *
   * <p>Most of the configuration that can be done on an {@link
   * Cloptus.Opt} can be done using arguments to the @OptTarget
   * annotation.
   *
   * <p>Once you've defined your annotations, just call {@link
   * Cloptus.OptSet#populateMembers(Object,String[])}, and it'll do
   * all the work for you.
   *
   * <pre class="code"> ...
   * // --- make an object whose members are annotated with @OptTarget.
   * Object targetObj = ...; 
   *
   * // --- parse the command line arguments.  If there's an error,
   * //   - or the user requests help, 'populateMembers' will return null.
   * if (OptSet.populateMembers (targetObj, args) == null) return;
   *
   * // --- and then, use your targetObj -- it's all filled out.
   * ...</pre>
   *
   * <p>For a more complete example, see the source code for {@link
   * Cloptus.AnnotationDemo}.
   *
   * <p>If you want to do more sophisticated option specification or
   * processing, you can use {@link Cloptus.OptSet} and {@link
   * Cloptus.Opt} directly.  See their documentation for details.
   *
   * <p>TODO: does not currently support Collection or array
   * members.  This should be fixed.
   *
   * <p>TODO: does not currently support calling convenience getters.
   * This should be fixed.
   *
   * @see OptSet#populateMembers(Object,String[])
   **/
  @Documented
  @Retention (RetentionPolicy.RUNTIME)
  @Target({ElementType.FIELD, ElementType.METHOD})
  public @interface OptTarget {

    /**
     * <p>Specifies the subtype of Opt to use for the annotated member.
     *
     * <p>Optional: if not specified, Cloptus will guess the type of
     * the option based on the argument type of the member according
     * to the following algorithm:
     *
     * <ul>
     *
     *   <li><b>String: </b>  StringOpt
     *   <li><b>Integer/int: </b>  IntegerOpt
     *   <li><b>Double/double: </b>  DoubleOpt
     *   <li><b>Boolean/boolean: </b>  BooleanOpt
     *   <li><b>Date: </b>  DateOpt
     *   <li><b>Enum&lt;E&gt;: </b>  EnumOpt&lt;E&gt;
     *   <li><b>File: </b>  FileOpt<T>
     *   <li><b>URI: </b>  UriOpt<T>
     *
     * </ul>
     *
     * <p>For fields, the argument type is the type of the field.  For
     * methods (which must have only one parameter), the argument type
     * is the type of the first parameter.
     **/
    public Class<?> optType () default NotSpecifiedOpt.class;

    /**
     * <p>Specifies the long name(s) to use for this option.
     *
     * <p>If not specified, the long name will default to the name of
     * the field or the setter.
     *
     * <p>Note: if the setter is of the form 'setFoo', then the name
     * will be 'foo'.  All other method names pass through unscathed.
     *
     * <p>Long-named options are specified using the <tt>--LONGNAME
     * ARG</tt> syntax at the command line.
     *
     * @see Cloptus.Opt#name(String)
     **/
    public String[] name () default {};

    /**
     * <p>Specifies the short (single-character) name(s) to use for
     * this option.
     *
     * <p>If not specified, the option will have no short names.
     *
     * <p>Short-named options are specified using <tt>-X ARG</tt>
     * syntax at the command line, and can be multiply specified like
     * this: <tt>-XYZ ARG_X ARG_Y ARG_Z</tt>
     *
     * @see Cloptus.Opt#shortName(char)
     **/
    public char[] shortName () default {};

    /**
     * <p>Specifies that this option can be specified with no name
     * (i.e. - positionally)
     *
     * <p>If not specified, you can't specify this option without a name.
     *
     * @see Cloptus.Opt#positional(boolean)
     **/
    public boolean positional () default false;

    /**
     * <p>If true, specifies that this option is required: an error
     * will be raised if option parsing completes and this option is
     * not specified.
     *
     * <p>If not specified, the option is optional.
     *
     * @see Cloptus.Opt#required(boolean)
     **/
    public boolean required () default false;

    /**
     * <p>Specifies the <em>metavar</em> to use in help text for this
     * option.  Usage information for an option looks like <tt>--name
     * METAVAR</tt>.
     *
     * <p>The metavar should describe the valid values for the
     * argument, using a single word.
     *
     * <p>If not specified, the metavar is derived from the type of
     * the option use.  For example, a StringOpt has the metavar
     * STRING.  A FileOpt has the metavar FILE.
     *
     * @see Cloptus.Opt#metavar(String)
     **/
    public String metavar () default "";

    /**
     * <p>Specifies the help text associated with this option.  This
     * text is printed if the user specifies the <tt>--help/-h/-?</tt>
     * option at the command line.
     *
     * <p>If not specified, this option has no help text.
     *
     * @see Cloptus.Opt#description(String)
     **/
    public String description () default "";
  }

  // -------------------------------------------------------------------------
  // ---- OptSet -------------------------------------------------------------
  // -------------------------------------------------------------------------

  /**
   * <p>A set of options, built using a builder API, and then used for
   * parsing.
   *
   * <p><tt>OptSet</tt> is the container in which instances of {@link
   * Opt} live, and is responsible for parsing String[] arguments into
   * {@link ParsedArgs}
   *
   * <p><b>Note: </b> if you want the easiest possible way of parsing
   * options, see the {@link OptTarget} annotation.  Using OptSets and
   * Opts directly is much more powerful, but 99% of programs don't
   * need that kind of power.
   *
   * <p>Using an OptSet is very straightforward.  Probably the easiest
   * way of demonstrating that is to show a complete use case.
   *
   * <pre class="code"> ...
   * // --- Create an instance of an OptSet.
   * OptSet opts = new OptSet ();
   *
   * // --- Create and add one or more options, customizing as necessary.
   * StringOpt mystringOpt = new StringOpt (opts, "mystring");
   *
   * // --- Run the option parser: this is where the work gets done.
   * ParsedArgs parsedArgs = opts.run (args);
   *
   * // --- If 'run' prints help text or an error, it returns <tt>null</tt>
   * if (parsedArgs == null) return;
   *
   * // --- Then, just use the options.
   * String mystring = mystringOpt.get (parsedArgs);
   * ...</pre>
   *
   * <p>There are many options you can set on {@link Opt} and its
   * subtypes, and many ways of extracting the values.  Your best bet
   * is to look at {@link Opt}'s documentation for more details.
   *
   * @see Opt
   **/
  public static class OptSet {

    HelpOpt helpOpt;

    String command = null;

    String description = null;
    
    Map<String,Opt<?>> optsByName = new LinkedHashMap<String,Opt<?>> ();

    boolean compiled = false;

    Map<String,Opt<?>> optsByLongName = new LinkedHashMap<String,Opt<?>> ();
    Map<Character,Opt<?>> optsByShortName =
      new LinkedHashMap<Character,Opt<?>> ();
    Opt<?> positionalOpt = null;

    // ---- Construction ----------------------------------------------------

    /**
     * <p>Constructs a new instance of an OptSet.
     *
     * <p>This is the principle interface you use: add options to this
     * set using <code>new FooOpt (opts, "name")</code>.
     **/
    public OptSet ()
    {
      addDefaultOptions ();
    }

    /**
     * <p>Called by the constructor.
     **/
    protected void addDefaultOptions ()
    {
      helpOpt = (HelpOpt)
        new HelpOpt (this, "help")
        .shortName ('h').shortName ('?')
        .description ("Print an informative help message.");
    }

    /**
     * <p>Called by the constructor of Opt
     **/
    protected void addOpt (Opt<?> opt)
    { optsByName.put (opt.getName (), opt); }

    // ---- Builder API -----------------------------------------------------

    /**
     * <p>Specifies the command name for usage.
     *
     * <p>Optional.  If left unspecified, Cloptus tries to guess the
     * command using the stack trace.
     **/
    public OptSet command (String v) { command = v; return this; }

    /**
     * <p>Specifies the program's description, for help text.
     **/
    public OptSet description (String v) { description = v; return this; }

    // ----------------------------------------------------------------------

    /**
     * <p>Reflectively examines annotations in the given type, and
     * defines options for each of them.
     **/
    public OptSet defineOptsForType (Class<?> type)
    {
      // --- examine public fields.
      for (Field f : type.getFields ())
        {
          OptTarget optTarget = f.getAnnotation (OptTarget.class);
          if (optTarget != null) Opt.make (this, optTarget, f);
        }

      // --- examine public methods.
      for (Method m : type.getMethods ())
        {
          OptTarget optTarget = m.getAnnotation (OptTarget.class);
          if (optTarget != null) Opt.make (this, optTarget, m);
        }

      return this;
    }


    // ----------------------------------------------------------------------

    /**
     * <p>Returns a collection of all the options specified.
     **/
    public Collection<Opt<?>> getOpts () { return optsByName.values (); }

    /**
     * <p>Fetches an option by canonical name.
     **/
    public <T extends Opt<?>> T getByName (String longName, Class<T> optType)
    { return optType.cast (optsByName.get (longName.toLowerCase ())); }

    /**
     * <p>Fetches an option by long name, returning the specified type.
     **/
    public <T extends Opt<?>> T getByLongName (String longName,Class<T>optType)
    { return optType.cast (getByLongName (longName)); }

    /**
     * <p>Fetches an option by long name, don't care what type.
     **/
    public Opt<?> getByLongName (String longName)
    { return optsByLongName.get (longName.toLowerCase ()); }

    /**
     * <p>Fetches an option by short name, returning the specified type.
     **/
    public <T extends Opt<?>> T getByShortName (char shortName,Class<T>optType)
    { return optType.cast (getByShortName (shortName)); }

    /**
     * <p>Fetches an option by short name, don't care what type.
     **/
    public Opt<?> getByShortName (char shortName)
    { return optsByShortName.get (shortName); }

    /**
     * <p>Fetches the (single) positional opt, returning the specified type.
     **/
    public <T extends Opt<?>> T getPositionalOpt (Class<T> optType)
    { return optType.cast (getPositionalOpt ()); }

    /**
     * <p>Fetches the (single) positional opt, don't care what type.
     **/
    public Opt<?> getPositionalOpt ()
    { return positionalOpt; }

    /**
     * <p>Fetches the help option (associated with --help)
     **/
    public HelpOpt getHelpOpt () { return helpOpt; }

    /**
     * <p>Fetch the command for the usage string.
     *
     * <p>If unspecified, searches the current thread's stack trace to
     * try to find the associated Class.
     **/
    public String getCommand ()
    {
      // --- if somebody overrides it, of course, take that one.
      if (command != null) return command;

      // XXX can we get the command that invoked us from Java?

      // XXX ensure that the stack trace element has method 'main'.

      // --- fetch it from the stack trace.
      StackTraceElement[] stackTrace = new Throwable ().getStackTrace ();
      return "java " + stackTrace[stackTrace.length-1].getClassName ();
    }

    /**
     * <p>Fetch the specified program description.
     **/
    public String getDescription () { return description; }

    // ----------------------------------------------------------------------

    /**
     * <p>Defines and runs an instance of OptSet given an annotated
     * object.
     *
     * <ul>
     *   <li>Defines options based on @{@link Cloptus.OptTarget}
     *   annotations on members of the type of <tt>targetObject</tt>
     *
     *   <li>parses the given command line arguments, and
     *
     *   <li>populates <tt>targetObject</tt> with the result of the parse.
     * </ul>
     *
     * <p>Convenience method to run if you're using @OptTarget
     * annotations.
     *
     * @see #defineOptsForType(Class)
     * @see #run(PrintWriter,String[])
     * @see Cloptus.ParsedArgs#populateMembers(Object)
     **/
    public static ParsedArgs populateMembers (Object targetObject,
                                              String... args)
    {
      OptSet opts = new OptSet ();

      opts.defineOptsForType (targetObject.getClass ());

      ParsedArgs parsedArgs = opts.run (args);
      if (parsedArgs == null) return null;

      parsedArgs.populateMembers (targetObject);

      return parsedArgs;
    }


    /**
     * <p>Parses the given command line arguments, and returns a new
     * ParsedArgs object containing the results of the parse.
     *
     * <p>Convenience method: runs the option parser using System.err as
     * the help/error stream.
     *
     * @see #run(PrintWriter,String[])
     **/
    public ParsedArgs run (String... args) { return run (System.err, args); }

    /**
     * <p>Parses the given command line arguments, writing errors and
     * help output to the given OutputStream.
     * 
     * <p>Convenience method for using PrintStreams.  {@link
     * System#out} is not a PrintWriter, and so calling {@link
     * #run(PrintWriter,String[])} directly is inconvenient.
     *
     * @see #run(PrintWriter,String[])
     **/
    public ParsedArgs run (OutputStream out, String... args)
    { return run (new PrintWriter (out, true), args); }

    /**
     * <p>Runs this option parser.
     *
     * <p>This high-level interface is intended to be run from the main()
     * method of a program.
     *
     * <p>It:
     * <ul>
     *   <li>parses the args
     *
     *   <li>if there are any errors, prints an informative message,
     *   usage, and reuturns null.
     *
     *   <li>if the user requested help, prints the full, detailed, usage,
     *   and returns null.
     *
     *   <li>otherwise, returns the ParsedArgs.
     * </ul>
     *
     * @see #run(String[])
     * @see #run(OutputStream,String[])
     * @see #parse(String[])
     * @see Cloptus.OptParseException
     * @see #printHelp(PrintWriter)
     * @see Cloptus.ParsedArgs#isHelpRequested()
     **/
    public ParsedArgs run (PrintWriter out, String... args)
    {
      ParsedArgs r;
      try {
        r = parse (args);
      } catch (OptParseException ex) {
        out.println ("ERROR: "+ex.getMessage ());
        out.println ();
        printHelp (out);

        return null;
      }

      if (r.isHelpRequested ())
        {
          printHelp (out);
          return null;
        }

      return r;
    }

    /**
     * <p>Parse the arguments.
     *
     * <p>XXX document me
     *
     * @throws OptParseException if there are any errors during
     * parsing.
     **/
    public ParsedArgs parse (String... args)
    {
      // --- compile the options.
      compile ();

      // --- make the option reader.
      OptReader in = new OptReader (args);

      // --- build the return value.
      ParsedArgs r = new ParsedArgs (this, args);

      // --- parse using the option grammar
      parseOptExprs (in, r);

      // --- check to ensure that all the global required/list/etc
      // --- things are correct.
      r.validate ();

      // --- woo hoo!  all done!
      return r;
    }

    // ----------------------------------------------------------------------

    /**
     * <p>Compiles the option specifiers contained in this OptSet object.
     *
     * <p>XXX document me
     *
     * @throws ArgCompilationException if there are any errors in the
     * specification.
     **/
    public void compile ()
    {
      if (compiled) return;

      // --- build the registry.
      for (Opt<?> a : optsByName.values ())
        {
          for (String longName : a.getLongNames ())
            {
              if (optsByLongName.get (longName) != null)
                throw new OptCompileException
                  ("Multiple opts with same name '"+longName+"': "
                   +positionalOpt+" and "+a);
              optsByLongName.put (longName.toLowerCase (), a);
            }

          for (char shortName : a.getShortNames())
            {
              if (optsByShortName.get (shortName) != null)
                throw new OptCompileException
                  ("Multiple opts with same short name '"+shortName+"': "
                   +positionalOpt+" and "+a);
              optsByShortName.put (shortName, a);
            }

          if (a.isPositional ())
            {
              if (positionalOpt != null)
                throw new OptCompileException
                  ("Multiple no-named opts: "+positionalOpt+" and "+a);
              positionalOpt = a;
            }
        }

      compiled = true;
    }

    // ---- Grammar ---------------------------------------------------------

    /**
     * <pre>( [optExpr] )* ("--" | EOS)</pre>
     **/
    protected void parseOptExprs (OptReader in, ParsedArgs r)
    {
      if (DebugParse) System.err.println ("paseOptExprs...");
      // --- read next opt until EOS
      while (in.peek () != null)
        {
          // --- end of tokens on '--'
          if (in.peek ().equals ("--")) break;

          // --- read option expression.
          r.addOpts (parseOptExpr (in));
        }
    }

    /**
     * <pre>( [longOpt] | [shortOpts] | [positionalOpt] | [argfile] )</pre>
     **/
    protected List<ParsedArg> parseOptExpr (OptReader in)
    {
      String optName = in.peek ();

      if (DebugParse) System.err.println ("paseOptExpr ["+optName+"]");

      if (optName.startsWith ("@"))
        return parseArgFile (in);

      // --- long-named opt
      else if (optName.startsWith ("--"))
        return parseLongOpt (in);

      // --- short-named opt list
      else if (optName.startsWith ("-") && optName.length () > 1)
        return parseShortOpts (in);

      // --- positional opt
      else
        return parsePositionalOpt (in);
    }

    protected List<ParsedArg> parseArgFile (OptReader in)
    {
      String argFileSpec = in.read ();

      // --- strip '@'
      argFileSpec = argFileSpec.substring (1);

      // --- now it's a filename.  Read it, and stick it into the stream...
      System.out.println
        ("XXX: should be reading and inserting file: "+argFileSpec);

      return new ArrayList<ParsedArg> ();
    }

    /**
     * <pre>( "--NAME" [arg] | "--NAME="[arg] )</pre>
     **/
    protected List<ParsedArg> parseLongOpt (OptReader in)
    {
      // --- consume opt name.
      String rawOptName = in.read ();

      if (DebugParse) System.err.println ("parseLongOpt ["+rawOptName+"]");

      String optName = rawOptName;

      // --- cope with '=' syntax.
      // XXX test
      int eqIdx = rawOptName.indexOf ('=');
      if (eqIdx != -1)
        {
          optName = rawOptName.substring (0, eqIdx);
          String newArg = rawOptName.substring (eqIdx+1);
          in.prependArg (newArg);
        }

      // --- lookup as long name.
      Opt<?> opt = getByLongName (optName.substring (2));
      if (opt == null)
        throw new OptParseException
          ("Don't understand option '"+optName+"'");

      // --- allow it to parse its own arguments.
      List<ParsedArg> r = new ArrayList<ParsedArg> ();
      r.add (new ParsedArg (opt, optName, opt.parseArg (in), false));
      return r;
    }

    /**
     * <pre>"-ABCD" ([arg])*</pre>
     **/
    protected List<ParsedArg> parseShortOpts (OptReader in)
    {
      // --- (possibly multiple) short-named opts

      // --- consume opt name.
      String optList = in.read ();

      if (DebugParse) System.err.println ("parseShortOpts ["+optList+"]");

      List<ParsedArg> r = new ArrayList<ParsedArg> ();

      // --- iterate through the characters.
      for (char shortName : optList.substring (1).toCharArray ())
        {
          if (DebugParse) System.err.println ("parseShortOpt ["+shortName+"]");

          // --- lookup as short name.
          Opt<?> opt = getByShortName (shortName);
          if (opt == null)
            throw new OptParseException
              ("Don't understand option '-"+shortName
               +"' in option list '"+optList+"'");

          // --- allow it to parse its own arguments.
          r.add (new ParsedArg (opt, String.valueOf (shortName),
                                opt.parseArg (in), false));
        }

      return r;
    }

    /**
     * <pre>[arg]</pre>
     **/
    protected List<ParsedArg> parsePositionalOpt (OptReader in)
    {
      String possibleArg = in.peek ();

      if (DebugParse)
        System.err.println ("parsePositionalOpt ["+possibleArg+"]");

      Opt<?> opt = getPositionalOpt ();
      if (opt == null)
        throw new OptParseException
          ("Don't understand option '"+possibleArg
           +"': no positional arguments allowed.");

      // --- allow it to parse its own arguments.
      List<ParsedArg> r = new ArrayList<ParsedArg> ();
      r.add (new ParsedArg (opt, null, opt.parseArg (in), false));
      return r;
    }

    // -----------------------------------------------------------------------

    /**
     * <p>Prints a usage string to the given PrintWriter.
     **/
    public void printUsage (PrintWriter out)
    {
      out.print (getCommand ());
      for (Opt<?> o : getOpts ())
        if (o.isRequired ())
          {
            out.print (" ");
            o.printUsage (out);
          }
    }

    /**
     * <p>Prints the help screen to the given PrintWriter.
     **/
    public void printHelp (PrintWriter out)
    {
      out.print ("Usage: ");
      printUsage (out);
      out.println ();

      if (getDescription () != null)
        WordWrap.wordwrap (out, getDescription ());

      out.println ();

      printOptionHelp (out);
    }

    /**
     * <p>Prints detailed help for all the specified options.
     **/
    public void printOptionHelp (PrintWriter out)
    {
      for (Opt<?> opt : getOpts ()) opt.printOptionHelp (out);
    }
  }

  // -------------------------------------------------------------------------
  // ---- Opt ----------------------------------------------------------------
  // -------------------------------------------------------------------------

  /**
   * <p>Option base class.  See various subtypes for real options.
   *
   * <p>Defines the interface for parsing option arguments.
   *
   * <p><b>Note: </b> if you want the easiest possible way of parsing
   * options, see the {@link OptTarget} annotation.  Using OptSets and
   * Opts directly is much more powerful, but 99% of programs don't
   * need that kind of power.
   *
   * <p>See {@link OptSet} for more information on what to do with
   * Opts once you've created them.
   *
   * <h3>Constructing an Opt</h3>
   *
   * <p>The constructor for Opt takes an instance of {@link OptSet}, and
   * the constructor adds the Opt to the OptSet.  This allows for the
   * incredibly succinct code pattern:
   *
   * <pre class="code">
   * StringOpt mystring = new StringOpt (opts, "mystring");
   * </pre>
   *
   * <h3>Customizing an Opt</h3>
   *
   * <p>Opt defines a builder API and state for:
   * <ul>
   *   <li>the Opt's {@link #defaultValue(Object)}
   *
   *   <li>various types of names ({@link #name(String)}, {@link
   *   #shortName(char)}, {@link #positional(boolean)})
   *
   *   <li>whether the option is {@link #required(boolean)}, or is a
   *   {@link #list(boolean)}.
   *
   *   <li>usage information: the {@link #metavar(String)} for the
   *   argument, and the {@link #description(String)}.
   * </ul>
   *
   * <p>An example of using some more sophisticated options:
   *
   * <pre class="code">
   * // --- defines a required option '--mystring STRING' that can also
   * //   - be specified as -s STRING, and has help text.
   * StringOpt mystring = new StringOpt (opts, "mystring")
   *    .shortName ('s').required (true)
   *    .description ("A handy string");
   * </pre>
   *
   * <h3>Fetching results</h3>
   *
   * <p>Opt defines generic methods to fetch the object or objects
   * specified by the user as the argument of the option.
   *
   * <p>If the option is a singleton (i.e. - not a list), then {@link
   * #get(Cloptus.ParsedArgs)} is used.
   *
   * <p>If the option is a <em>list</em> Opt, then {@link
   * #getList(Cloptus.ParsedArgs)} or {@link
   * #getArray(Cloptus.ParsedArgs)} is used.
   *
   * <pre class="code">
   * System.out.println ("Got --mystring == "+mystring.get (parsedArgs));
   * </pre>
   *
   * <p>Because these methods are generic, you will get the correct
   * type at compile time: <tt>StringOpt.get</tt> is typed as String,
   * for example.
   *
   * <p>Most of the subtypes of Opt define convenience getters for
   * particular values.  {@link Cloptus.FileOpt}, for example, defines
   * {@link Cloptus.FileOpt#getStringContent(Cloptus.ParsedArgs)} that
   * returns the contents of the file as a String, among many others.
   * See particular subtypes for details.
   *
   * @see OptSet
   **/
  public static abstract class Opt<A> {

    /*
      Notes on the genericity of Opt

      This class is generic over the argument type of this
      option. (i.e. - StringOpt should be generic over String, etc).

      This allows methods like get(ParsedArgs) and
      defaultValue(Object) to be typed correctly.

      Also, note that the entire builder API is covariant: it is
      overridden in every Opt subtype.  This somewhat irritating code
      pattern allows the type of a series of builder method calls to
      be typed correctly, allowing this pattern:

        StringOpt mystring = new StringOpt (opts, "mystring")
          .positional (true).shortName ('m').description ("blah blah");

      Without the covariant overrides, you would need to cast the
      result before assigning it.
     */

    final OptSet opts;
    final String name;
    final Class<A> argType;

    A defaultValue;

    List<String> longNames = new ArrayList<String> ();
    List<Character> shortNames = new ArrayList<Character> ();
    boolean positional = false;

    boolean required = false;
    boolean list = false;

    String metavar = null;
    String description = null;

    Member target = null;

    /**
     * <p>Makes a new option.
     *
     * @param _opts - the option list to add this option to (the
     * constructor calls addOpt as a side effect).
     *
     * @param _name - the CANONICAL name for this option.  Used in
     * various APIs to fetch the option reflectively.  The canonical
     * name is automatically added as a long name for this option (see
     * {@link #name(String)})
     *
     * @param _argType - a Class containing the argument type for this
     * option.  i.e. - for a StringOpt, this would be String.class.
     * Necessary for some of the reflective calls this guy does.
     *
     * @throws NullPointerException if any of the arguments are null:
     * everything in the constructor is required.
     **/
    public Opt (OptSet _opts, String _name, Class<A> _argType)
    {
      if (_opts == null)
        throw new NullPointerException ("_opts cannot be null");
      if (_name == null)
        throw new NullPointerException ("_name cannot be null");
      if (_argType == null)
        throw new NullPointerException ("_argType cannot be null");

      opts = _opts;
      name = _name;
      argType = _argType;
      longNames.add (_name);
      opts.addOpt (this);
    }

    // ---- Builder API -----------------------------------------------------

    /**
     * <p>Sets the default value for this option.
     *
     * <p>The type of the default value is dependent on the particular
     * subtype of Opt.
     **/
    public Opt<A> defaultValue (A v) { defaultValue = v; return this; }

    /**
     * <p>Adds an additional (long) name for this opt.
     *
     * <p>If not specified, this option will have only the (canonical)
     * name passed in the constructor.  All Opts have at least one
     * long name.
     *
     * <p>Long-named options are specified using the <tt>--LONGNAME
     * ARG</tt> syntax at the command line.
     *
     * @see Cloptus.OptTarget#name()
     **/
    public Opt<A> name (String name) { longNames.add (name); return this; }

    /**
     * <p>Adds a short name for this opt.
     *
     * <p>If not specified, the option will have no short names.
     *
     * <p>Short-named options are specified using <tt>-X ARG</tt>
     * syntax at the command line, and can be multiply specified like
     * this: <tt>-XYZ ARG_X ARG_Y ARG_Z</tt>
     *
     * @see Cloptus.OptTarget#shortName()
     **/
    public Opt<A> shortName (char name) { shortNames.add (name); return this; }

    /**
     * <p>Specifies whether this opt can be specified with no name
     * (i.e. - positionally)
     *
     * <p>If not specified, you can't specify this option without a name.
     *
     * @see Cloptus.OptTarget#positional()
     **/
    public Opt<A> positional (boolean v) { positional = v; return this; }

    /**
     * <p>If true, specifies that this option is required: an error
     * will be raised if option parsing completes and this option is
     * not specified.
     *
     * <p>If not specified, the option is optional.
     *
     * @see Cloptus.OptTarget#required()
     **/
    public Opt<A> required (boolean v) { required = v; return this; }

    /**
     * <p>If true, specifies that this option is a <em>list</em>: this
     * means that you can specify this option more than once, and the
     * result of parsing the option will be a Collection or array.
     *
     * <p>If not specified, the option is a singleton: an error will
     * be raised if you specify the option more than once.
     *
     * <p>When using {@link OptTarget}, Collection and array-typed
     * members will automatically be list Opts.
     **/
    public Opt<A> list (boolean v) { list = v; return this; }

    /**
     * <p>Specifies the <em>metavar</em> to use in help text for this
     * option.  Usage information for an option looks like <tt>--name
     * METAVAR</tt>.
     *
     * <p>The metavar should describe the valid values for the
     * argument, using a single word.
     *
     * <p>If not specified, the metavar is derived from the type of
     * the option use.  For example, a StringOpt has the metavar
     * STRING.  A FileOpt has the metavar FILE.
     *
     * @see Cloptus.OptTarget#metavar()
     **/
    public Opt<A> metavar (String v) { metavar = v; return this; }

    /**
     * <p>Specifies the help text associated with this option.  This
     * text is printed if the user specifies the <tt>--help/-h/-?</tt>
     * option at the command line.
     *
     * <p>If not specified, this option has no help text.
     *
     * @see Cloptus.OptTarget#description()
     **/
    public Opt<A> description (String v) { description = v; return this; }

    // ---- Getters ---------------------------------------------------------

    /**
     * <p>Gets the principle name of this opt (used in help, as well
     * as in APIs to fetch the opt reflectively)
     *
     * <p>This is the canonical name passed in the constructor.
     **/
    public String getName () { return name; }

    /**
     * Returns the Class object representing the arg type of this Opt.
     **/
    public Class<A> getArgType () { return argType; }

    /**
     * <p>Returns the specified default value for this argument.
     **/
    public A getDefaultValue () { return defaultValue; }

    /**
     * <p>Returns the list of long names for this argument.  This
     * includes the canonical name.
     **/
    public List<java.lang.String> getLongNames () { return longNames; }

    /**
     * <p>Returns the list of short names for this argument.
     **/
    public List<java.lang.Character> getShortNames () { return shortNames; }

    /**
     * <p>Returns true if this option can be specified without a name.
     * There can only be one positional option.
     **/
    public boolean isPositional () { return positional; }

    /**
     * <p>Returns true if this option is required.  A parse exception is
     * raised if it is missing.
     **/
    public boolean isRequired () { return required; }

    /**
     * <p>Returns true if this option may be specified more than once.
     **/
    public boolean isList () { return list; }

    /**
     * <p>Return the name of the metavariable to use in usage strings.
     * If 'null', this option takes no argument.
     **/
    public String getMetavar () { return metavar; }

    /**
     * <p>Returns the description for this option, used in help text.
     **/
    public String getDescription () { return description; }


    // ---- Parse -----------------------------------------------------------

    /**
     * <p>Parse the argument for this option.
     *
     * <p>Subtypes must override this method to parse their arguments.
     **/
    protected abstract A parseArg (OptReader in);

    // ---- Fetch Return Value ----------------------------------------------

    /**
     * <p>Fetch the (single) specified value for this option.  Can return
     * null if the option was not found and no default was specified.
     *
     * <p>One of the three default opt target fetchers.
     *
     * @throws OptUseException if more than one value exists (as can
     * be, for 'list'-typed options).
     *
     * @see #getList(Cloptus.ParsedArgs)
     * @see #getArray(Cloptus.ParsedArgs)
     **/
    public A get (ParsedArgs parsedArgs)
    { return parsedArgs.getValue (this, argType, defaultValue); }

    /**
     * <p>Fetch all the specified values for this option.
     *
     * <p>If a default value was specified, and this list would
     * otherwise be empty, a singleton list containing the default
     * value is returned.
     *
     * <p>One of the three default opt target fetchers.
     *
     * @see #get(Cloptus.ParsedArgs)
     * @see #getArray(Cloptus.ParsedArgs)
     **/
    public List<A> getList (ParsedArgs parsedArgs)
    { return parsedArgs.getValues (this, argType, defaultValue); }

    /**
     * <p>Fetch all of the specified values for this option, as a Java
     * array.
     *
     * <p>One of the three default opt target fetchers.
     *
     * @see #get(Cloptus.ParsedArgs)
     * @see #getList(Cloptus.ParsedArgs)
     **/
    @SuppressWarnings(value={"unchecked"})
    public A[] getArray (ParsedArgs parsedArgs)
    {
      List<A> list = getList (parsedArgs);

      // --- make a return array (XXX how to remove unchecked warning???)
      A[] r = (A[])java.lang.reflect.Array.newInstance(argType, list.size ());

      return list.toArray (r);
    }

    // ---- Wiring for OptTarget --------------------------------------------

    /**
     *
     **/
    protected static Opt<?> make (OptSet opts, OptTarget optTarget, Member m)
    {
      // --- figure out the argument type.
      Class<?> argType = getMemberType (m);

      // --- work out the type of option to create.
      Class<?> optType = null;
      if (optType == null)
        {
          optType = optTarget.optType ();
          if (optType == NotSpecifiedOpt.class) optType = null;
        }
      if (optType == null) optType = getDefaultOptType (argType);
      if (!Opt.class.isAssignableFrom (optType))
        throw new ClassCastException
          ("Got non-Opt subtype for optType in @OptTarget "+optTarget);

      // --- figure out the option name.
      String optName;
      if (optTarget.name ().length > 0)
        optName = optTarget.name ()[0];
      else
        optName = getDefaultOptName (m);

      // --- make the option.
      Opt<?> r = make (optType, opts, optName, argType);

      if (argType.isArray ()) r.list (true);

      r.setTarget (m);
      r.initFromOptTarget (optTarget);
      return r;
    }

    /**
     *
     **/
    private static String getDefaultOptName (Member m)
    {
      String memberName = m.getName ();
      if (m instanceof Field)
        return memberName;
      else if (m instanceof Method)
        {
          if (memberName.startsWith ("set") && memberName.length () > 3)
            return
              Character.toLowerCase (memberName.charAt (3))
              + memberName.substring (4);
          else
            return memberName;
        }
      else
        {
          assert (false);
          return null;
        }
    }

    /**
     *
     **/
    private static Class<?> getMemberType (Member m)
    {
      if (m instanceof Field)
        return ((Field)m).getType ();
      else if (m instanceof Method)
        {
          Method method = (Method)m;
          // --- must be a 1-arg method
          Class<?>[] parameterTypes = method.getParameterTypes ();
          if (parameterTypes.length != 1)
            throw new OptCompileException
              ("@OptTarget can only be specified on methods with 1 parameter: "
               +"got "+parameterTypes.length+" parameters for method "+method);
          return parameterTypes[0];
        }
      else
        {
          assert (false);
          return null;
        }
    }

    /**
     * <p>Fetches the Class corresponding to the field or method type.
     *
     * @see Cloptus.OptTarget#optType()
     **/
    public static Class<?> getDefaultOptType (Class<?> argType)
    {
      if (argType == String.class) return StringOpt.class;
      else if (argType == Integer.class || argType == int.class)
        return IntegerOpt.class;
      else if (argType == Double.class || argType == double.class)
        return DoubleOpt.class;
      else if (argType == Boolean.class || argType == boolean.class)
        return FlagOpt.class;
      else if (argType == Date.class) return DateOpt.class;
      else if (Enum.class.isAssignableFrom (argType)) return EnumOpt.class;
      else if (argType == File.class) return FileOpt.class;
      else if (argType == URI.class) return UriOpt.class;

      else if (argType.isArray ())
	return getDefaultOptType (argType.getComponentType ());

      else throw new OptCompileException
             ("No default option type for @OptTarget of type "+argType);
    }

    /**
     *
     **/
    private static
      Opt<?> make (Class<?> optType, OptSet opts, String optName,
                   Class<?> argType)
    {
      // --- try to find constructor (opts, name, argType)
      try {
        Constructor<?> constructor = optType.getConstructor (OptSet.class,
                                                             String.class,
                                                             Class.class);
        return (Opt<?>)constructor.newInstance (opts, optName, argType);
      } catch (IllegalAccessException ex) {
        throw new OptCompileException
          ("Cannot access constructor (OptSet, String, Class<A>)", ex);
      } catch (InstantiationException ex) {
        throw new OptCompileException
          ("Option type "+optType+" is abstract", ex);
      } catch (InvocationTargetException ex) {
        throw new OptCompileException
          ("Error executing constructor (OptSet, String, Class<A>)", ex);
      } catch (ExceptionInInitializerError ex) {
        throw new OptCompileException
          ("Error executing constructor (OptSet, String, Class<A>)", ex);
      } catch (ClassCastException ex) {
        throw new OptCompileException
          ("Option type "+optType+
           " incorrect in constructor (OptSet, String, Class<A>)", ex);
      } catch (NoSuchMethodException ex) {
        // --- fallthrough.
      }

      // --- otherwise, try to find constructor (opts, name)
      try {
        Constructor<?> constructor = optType.getConstructor(OptSet.class,
                                                            String.class);
        return (Opt<?>)constructor.newInstance (opts, optName);
      } catch (IllegalAccessException ex) {
        throw new OptCompileException
          ("Cannot access constructor (OptSet, String)", ex);
      } catch (InstantiationException ex) {
        throw new OptCompileException
          ("Option type "+optType+" is abstract", ex);
      } catch (InvocationTargetException ex) {
        throw new OptCompileException
          ("Error executing constructor (OptSet, String)", ex);
      } catch (ExceptionInInitializerError ex) {
        throw new OptCompileException
          ("Error executing constructor (OptSet, String)", ex);
      } catch (ClassCastException ex) {
        throw new OptCompileException
          ("Option type "+optType+
           " incorrect in constructor (OptSet, String)", ex);
      } catch (NoSuchMethodException ex) {
        // --- fallthrough
      }

      throw new OptCompileException ("No reasonable constructor found "+
                                     "for Opt subtype "+optType);
    }

    /**
     *
     **/
    public Opt<A> initFromOptTarget (OptTarget target)
    {
      assert (getClass () == target.optType ());

      for (String name : target.name ()) name (name);
      for (char shortName : target.shortName ()) shortName (shortName);
      positional (target.positional ());
      
      required (target.required ());
      // XXX set to list of argument is appropriate type.
      // list (target.list ());

      if (!"".equals (target.metavar ())) metavar (target.metavar ());
      if (!"".equals (target.description ()))
        description (target.description ());

      return this;
    }

    public Opt<A> setTarget (Member v)
    {
      if (!(v instanceof Field || v instanceof Method))
        throw new OptCompileException
          ("Bad target: expected either Field or Method, got "+v.getClass ());
      target = v;
      return this;
    }

    public void populateMember (Object targetObj,
                                ParsedArgs parsedArgs)
    {
      // --- don't bother if there is no target.
      if (target == null) return;

      // --- make sure the type is correct.
      Class<?> desiredTargetObjType = target.getDeclaringClass ();
      if (!desiredTargetObjType.isInstance (targetObj))
        throw new ClassCastException
          ("Bad target object: desired "+desiredTargetObjType.getClass ()
           +", got "+targetObj.getClass ());

      Class<?> argType = getMemberType (target);

      Object value;
      if (argType.isArray ())
	value = getArray (parsedArgs);
      else
	value = get (parsedArgs);

      // --- not specified: don't try to set it.
      if (value == null) return;

      // --- invoke the target (depending on its type)
      if (target instanceof Field)
        try {
          ((Field)target).set (targetObj, value);
        } catch (IllegalArgumentException ex) {
          throw new OptUseException
            ("Bad argument type for field: expected "
             +((Field)target).getType ()+", got "+value
             +" ("+value.getClass ()+")", ex);
        } catch (IllegalAccessException ex) {
          throw new OptUseException ("XXX", ex);
        }
      else if (target instanceof Method)
        try {
          ((Method)target).invoke (targetObj, value);
        } catch (IllegalArgumentException ex) {
          if (!target.getDeclaringClass ().isInstance (targetObj))
            throw new OptUseException
              ("Bad call to "+target.getName ()+": tried to use 'this' "+
               "of type "+targetObj+", wanted "+target.getDeclaringClass ());

          throw new OptUseException
            ("Something bad happened when trying to invoke "+target
             +", with this="+targetObj+", and arg="+value, ex);
        } catch (IllegalAccessException ex) {
          throw new OptUseException ("XXX", ex);
        } catch (InvocationTargetException ex) {
          throw new OptUseException ("XXX", ex);
        }
      else
        throw new OptUseException ("Bad target: "+target.getClass ());
    }

    // ---- Debug -----------------------------------------------------------
    
    /**
     * <p>Print this option as a String.
     *
     * <p>Defined as {@link #printSimpleUsage(PrintWriter)}
     **/
    public String toString ()
    {
      StringPrintWriter out = new StringPrintWriter ();
      printSimpleUsage (out);
      return out.toString ();
    }

    /**
     * <p>Print simple usage for this option (canonical long name, and
     * metavar).  Does not print arity information.
     **/
    public void printSimpleUsage (PrintWriter out)
    { printLongNameUsage (out, getName (), getMetavar ()); }

    /**
     * <p>Print usage for this argument, including arity specifiers for
     * list and non-required options.
     **/
    public void printUsage (PrintWriter out)
    {
      if (isRequired () && isList ())
        {
          out.print ("( ");
          printSimpleUsage (out);
          out.print (" )+");
        }
      else if (isList ())
        {
          out.print ("( ");
          printSimpleUsage (out);
          out.print (" )*");
        }
      else if (!isRequired ())
        {
          out.print ("[ ");
          printSimpleUsage (out);
          out.print (" ]");
        }
      else
        printSimpleUsage (out);
    }

    /**
     * <p>Prints all possible usage information for this option, in a
     * comma-seperated list.
     **/
    public void printExhaustiveUsage (PrintWriter out)
    {
      String metavar = getMetavar ();

      boolean first = true;

      // --- first, long names.
      for (String s : getLongNames ())
        {
          if (!first) out.print (", "); else first = false;
          printLongNameUsage (out, s, metavar);
        }

      // --- then, other short names.
      for (char s : getShortNames ())
        {
          if (!first) out.print (", "); else first = false;
          printShortNameUsage (out, s, metavar);
        }

      // --- then, positional usage.
      if (isPositional ())
        {
          if (!first) out.print (", "); else first = false;
          printPositionalUsage (out, metavar);
        }
    }

    /**
     * <p>Prints help for this option, including exhaustive usage, and
     * any help text associated with this option.
     **/
    public void printOptionHelp (PrintWriter out)
    {
      // --- print usage details to string.
      StringPrintWriter sout = new StringPrintWriter ();
      printExhaustiveUsage (sout);
      String usageDetails = sout.toString ();

      // --- indent description 30 characters.
      String IndentSpaces = "                              ";

      // --- if usage details < IndentSpaces
      if (usageDetails.length () < IndentSpaces.length ())
        {
          out.print (usageDetails);
          out.print (IndentSpaces.substring
                     (0, IndentSpaces.length () - usageDetails.length ()));
        }
      else
        {
          out.println (usageDetails);
          out.print (IndentSpaces);
        }

      WordWrap.wordwrap (out, getDescription (), IndentSpaces, false);
      out.println ();
    }

    /**
     * <p>Utility: prints simple usage for the given long name and metavar.
     **/
    public static void printLongNameUsage (PrintWriter out, String name, 
                                           String metavar)
    {
      out.print ("--");
      out.print (name);
      if (metavar != null)
        {
          out.print ("=");
          out.print (metavar);
        }
    }

    /**
     * <p>Utility: prints simple usage for the given short name and metavar.
     **/
    public static void printShortNameUsage (PrintWriter out, char name, 
                                            String metavar)
    {
      out.print ("-");
      out.print (name);
      if (metavar != null)
        {
          out.print (" ");
          out.print (metavar);
        }
    }

    /**
     * <p>Utility: prints simple usage for a positional opt with the
     * given metavar.
     **/
    public static void printPositionalUsage (PrintWriter out, String metavar)
    {
      if (metavar == null) out.print ("ARG");
      else                 out.print (metavar);
    }

  }

  // -------------------------------------------------------------------------
  // ---- Implementations of Opt ---------------------------------------------
  // -------------------------------------------------------------------------

  /**
   * <p>This is a gross hack to deal with the fact that null cannot be
   * a default for annotation members.
   **/
  private static abstract class NotSpecifiedOpt extends Opt<Object> {
    private NotSpecifiedOpt (OptSet _opts, String _name)
    { super (_opts, _name, Object.class); }
  }

  /**
   * <p>A String option.  The simplest possible option.
   **/
  public static class StringOpt extends Opt<String> {

    /**
     * <p>Make a StringOpt with the given name.
     **/
    public StringOpt (OptSet _opts, String _name)
    { super (_opts, _name, String.class); metavar ("STRING"); }

    /* Covariant builder methods: for convenient building. */
    public StringOpt defaultValue(String v){super.defaultValue(v);return this;}
    public StringOpt name (String name) { super.name (name); return this; }
    public StringOpt shortName (char name){super.shortName(name);return this; }
    public StringOpt positional (boolean v){super.positional(v); return this; }
    public StringOpt required (boolean v) { super.required (v); return this; }
    public StringOpt list (boolean v) { super.list (v); return this; }
    public StringOpt metavar (String v) { super.metavar (v); return this; }
    public StringOpt description (String v){super.description(v);return this; }

    /**
     * <p>Parse the argument.
     **/
    protected String parseArg (OptReader in) { return in.read (); }
  }

  /**
   * <p>An IntegerOpt: parses integers.
   *
   * <p>Supports 0x... (hex) 0b... (binary) and 0... (octal) constants.
   *
   * XXX May one day support SI suffixes (like K, M, etc)
   **/
  public static class IntegerOpt extends Opt<Integer> {

    /**
     * <p>Make an IntegerOpt with the given name.
     **/
    public IntegerOpt (OptSet _opts, String _name)
    { super (_opts, _name, Integer.class); metavar ("INTEGER"); }

    /* Covariant builder methods: for convenient building. */
    public IntegerOpt defaultValue(Integer v)
    {super.defaultValue(v);return this;}
    public IntegerOpt name (String name) { super.name (name); return this; }
    public IntegerOpt shortName (char name){super.shortName(name);return this;}
    public IntegerOpt positional (boolean v){super.positional(v); return this;}
    public IntegerOpt required (boolean v) { super.required (v); return this; }
    public IntegerOpt list (boolean v) { super.list (v); return this; }
    public IntegerOpt metavar (String v) { super.metavar (v); return this; }
    public IntegerOpt description (String v){super.description(v);return this;}

    /**
     * <p>Convenience method, gets the option result as a Java int.
     *
     * <p>Returns -1 if the value is not found.
     **/
    public int getInt (ParsedArgs parsedArgs)
    {
      Integer r = get (parsedArgs);
      if (r == null) return -1;
      return r;
    }

    /**
     * <p>Convenience method, gets the option result as a Java int array.
     **/
    public int[] getIntArray (ParsedArgs parsedArgs)
    {
      List<Integer> list = getList (parsedArgs);
      int[] r = new int[list.size ()];
      int idx = 0;
      for (int i : list) r[idx++] = i;
      return r;
    }

    /**
     * <p>Parse the next token as an Integer.
     *
     * <p>Includes support for various bases.
     **/
    protected Integer parseArg (OptReader in)
    {
      String valueStr = in.read ();

      // --- hexadecimal
      final int radix;
      if (valueStr.startsWith ("0x"))
        { valueStr = valueStr.substring (2); radix = 16; }
      // --- binary
      else if (valueStr.startsWith ("0b"))
        { valueStr = valueStr.substring (2); radix = 2; }
      // --- octal
      else if (valueStr.startsWith ("0"))
        { valueStr = valueStr.substring (1); radix = 8; }
      // --- decimal
      else
        { radix = 10; }

      // --- parse the value.
      int value;
      try {
        value = Integer.parseInt (valueStr, radix);
      } catch (NumberFormatException ex) {
        throw new OptParseException
          ("Bad value "+valueStr+" for option "+this
           +": expected integer.", ex);
      }

      return value;
    }
  }

  /**
   * <p>A DoubleOpt.  Allows specification of floating point values.
   **/
  public static class DoubleOpt extends Opt<Double> {

    /**
     * <p>Make a DoubleOpt with the given name.
     **/
    public DoubleOpt (OptSet _opts, String _name)
    { super (_opts, _name, Double.class); metavar ("DOUBLE"); }

    /* Covariant builder methods: for convenient building. */
    public DoubleOpt defaultValue(Double v){super.defaultValue(v);return this;}
    public DoubleOpt name (String name) { super.name (name); return this; }
    public DoubleOpt shortName (char name){super.shortName(name);return this;}
    public DoubleOpt positional (boolean v){super.positional(v); return this;}
    public DoubleOpt required (boolean v) { super.required (v); return this; }
    public DoubleOpt list (boolean v) { super.list (v); return this; }
    public DoubleOpt metavar (String v) { super.metavar (v); return this; }
    public DoubleOpt description (String v){super.description(v);return this;}

    /**
     * <p>Convenience method, gets the option result as a Java double.
     *
     * <p>Returns Double.NaN if the value is not found.
     **/
    public double getDouble (ParsedArgs parsedArgs)
    {
      Double r = get (parsedArgs);
      if (r == null) return Double.NaN;
      return r;
    }

    /**
     * <p>Convenience method, gets the option result as a Java double array.
     **/
    public double[] getDoubleArray (ParsedArgs parsedArgs)
    {
      List<Double> list = getList (parsedArgs);
      double[] r = new double[list.size ()];
      int idx = 0;
      for (double i : list) r[idx++] = i;
      return r;
    }

    /**
     * <p>Parses the next token as a Double value.
     **/
    protected Double parseArg (OptReader in)
    {
      String valueStr = in.read ();

      double value;
      try {
        value = Double.parseDouble (valueStr);
      } catch (NumberFormatException ex) {
        throw new OptParseException
          ("Bad value "+valueStr+" for option "+this
           +": expected doubleeger.", ex);
      }

      return value;
    }
  }

  /**
   * <p>A Boolean option: parses boolean values.
   *
   * <p>A BooleanOpt expects a single argument that is parsed as
   * "true" or "false".
   *
   * <p>{@link #get(Cloptus.ParsedArgs)} returns objects of type
   * java.lang.Boolean.
   *
   * <p>If you want a flag (with no argument), use {@link FlagOpt}
   *
   * @see Cloptus.Opt
   **/
  public static class BooleanOpt extends Opt<Boolean> {

    /**
     * <p>Make a BooleanOpt with the given name.
     **/
    public BooleanOpt (OptSet _opts, String _name)
    { super (_opts, _name, Boolean.class); metavar ("BOOLEAN"); }

    /* Covariant builder methods: for convenient building. */
    public BooleanOpt defaultValue(Boolean v)
    {super.defaultValue(v);return this;}
    public BooleanOpt name (String name) { super.name (name); return this; }
    public BooleanOpt shortName (char name){super.shortName(name);return this;}
    public BooleanOpt positional (boolean v){super.positional(v); return this;}
    public BooleanOpt required (boolean v) { super.required (v); return this; }
    public BooleanOpt list (boolean v) { super.list (v); return this; }
    public BooleanOpt metavar (String v) { super.metavar (v); return this; }
    public BooleanOpt description (String v){super.description(v);return this;}

    /**
     * <p>Convenience method, gets the option result as a Java boolean.
     *
     * <p>Returns false if the value is not found.
     **/
    public boolean getBoolean (ParsedArgs parsedArgs)
    {
      Boolean r = get (parsedArgs);
      if (r == null) return false;
      return r;
    }

    /**
     * <p>Convenience method, gets the option result as a Java boolean array.
     **/
    public boolean[] getBooleanArray (ParsedArgs parsedArgs)
    {
      List<Boolean> list = getList (parsedArgs);
      boolean[] r = new boolean[list.size ()];
      int idx = 0;
      for (boolean b : list) r[idx++] = b;
      return r;
    }

    /**
     * <p>Parses the next token as a boolean value.
     *
     * <p>If the first character (case insensitive) is '0' or 'f', the
     * value is false.
     *
     * <p>If the first character is '1' or 't', the value is true.
     **/
    protected Boolean parseArg (OptReader in)
    {
      String valueStr = in.read ();

      if (valueStr.length () == 0)
        throw new OptParseException ("Bad value '' for option "+this
                                     +": expected 'true' or 'false'.");

      switch (valueStr.toLowerCase ().charAt (0))
        {
        case '0': case 'f': return false;
        case '1': case 't': return true;
        default:
          throw new OptParseException ("Bad value '"+valueStr
                                       +"' for option "+this
                                       +": expected 'true' or 'false'.");
        }
    }
  }

  /**
   * <p>An option flag.
   *
   * <p>No arguments, returns 'true' if specified, and 'false' if not
   * specified.
   *
   * <p>If 'invert()' is called, will return 'false' if specified, and
   * 'true' if not specified.
   **/
  public static class FlagOpt extends BooleanOpt {

    /**
     * <p>The value to return if specified.
     **/
    Boolean valueIfSet = true;

    /**
     * <p>Make a new FlagOpt with the given name.
     **/
    public FlagOpt (OptSet _opts, String _name)
    { super (_opts, _name); metavar (null); defaultValue (false); }

    /**
     * <p>Invert this flag.
     *
     * <p>If called, this flag will return 'false' if specified, and
     * 'true' if not specified, the inverse of the normal behavior.
     **/
    public FlagOpt invert ()
    { defaultValue (true); valueIfSet = false; return this; }

    /* Covariant builder methods: for convenient building. */
    public FlagOpt defaultValue(Boolean v) {super.defaultValue(v);return this;}
    public FlagOpt name (String name) { super.name (name); return this; }
    public FlagOpt shortName (char name){super.shortName(name);return this;}
    public FlagOpt positional (boolean v){super.positional(v); return this;}
    public FlagOpt required (boolean v) { super.required (v); return this; }
    public FlagOpt list (boolean v) { super.list (v); return this; }
    public FlagOpt metavar (String v) { super.metavar (v); return this; }
    public FlagOpt description (String v){super.description(v);return this;}

    /**
     * <p>Parses no tokens: just returns the default value.
     **/
    protected Boolean parseArg (OptReader in) { return valueIfSet; }
  }

  /**
   * <p>A help option.  Added by default to the OptSet object.
   *
   * <p>This is just a FlagOpt with a special type.
   **/
  public static class HelpOpt extends FlagOpt {

    /**
     * <p>Make a new HelpOpt with the given name.
     **/
    public HelpOpt (OptSet _opts, String _name) { super (_opts, _name); }

    /* Covariant builder methods: for convenient building. */
    public HelpOpt defaultValue(Boolean v) {super.defaultValue(v);return this;}
    public HelpOpt name (String name) { super.name (name); return this; }
    public HelpOpt shortName (char name){super.shortName(name);return this;}
    public HelpOpt positional (boolean v){super.positional(v); return this;}
    public HelpOpt required (boolean v) { super.required (v); return this; }
    public HelpOpt list (boolean v) { super.list (v); return this; }
    public HelpOpt metavar (String v) { super.metavar (v); return this; }
    public HelpOpt description (String v){super.description(v);return this;}
  }

  /**
   * <p>A Date option: allows specification of Dates.
   *
   * <p>A DateOpt expects a single argument that is parsed as a
   * Gregorian Date.  This will change in the future: Cloptus will get
   * a Date parsing library that accepts a very flexible date syntax.
   *
   * <p>{@link #get(Cloptus.ParsedArgs)} returns objects of type
   * java.util.Date.
   *
   * <p>There are no convenience methods at this time.
   *
   * <p>TODO: Right now, uses <tt>java.util.Date.parse()</tt> to parse
   * the date, which sucks.  Write a new, flexible date parser for
   * this guy.
   **/
  public static class DateOpt extends Opt<Date> {

    /**
     * <p>Make a new DateOpt with the given name.
     **/
    public DateOpt (OptSet _opts, String _name)
    { super (_opts, _name, Date.class); metavar ("DATE"); }

    /* Covariant builder methods: for convenient building. */
    public DateOpt defaultValue(Date v) {super.defaultValue(v);return this;}
    public DateOpt name (String name) { super.name (name); return this; }
    public DateOpt shortName (char name){super.shortName(name);return this;}
    public DateOpt positional (boolean v){super.positional(v); return this;}
    public DateOpt required (boolean v) { super.required (v); return this; }
    public DateOpt list (boolean v) { super.list (v); return this; }
    public DateOpt metavar (String v) { super.metavar (v); return this; }
    public DateOpt description (String v){super.description(v);return this;}

    /**
     * <p>Parses the next token as a Date.
     **/
    @SuppressWarnings("deprecation")
    protected Date parseArg (OptReader in)
    {
      String value = in.read ();

      // XXX parse Date here in fancier way.
      try {
        // this deprecated method was WAY more awesome than normal date parsing.
        // that said, I really need to write/fetch a flexible human-friendly date parser.  This is nuts.
        return new Date (value);
      } catch (IllegalArgumentException ex) {
        throw new OptParseException ("Bad value '"+value+"' for option "+this
                                     +": expected date.", ex);
      }
    }
  }

  /**
   * <p>This is a cute one: an enumeration option, parses values from
   * a Java Enum object.
   *
   * <p>Expects a single argument that is parsed as one of the values
   * from a java.lang.Enum type.  Eventually, this parsing will be
   * case-insensitive.  Currently, it is case-sensitive (which makes
   * it hard for users).
   *
   * <p>{@link #get(Cloptus.ParsedArgs)} returns an instance of the
   * appropriate java.lang.Enum subtype.
   *
   * <p>TODO: make case insensitive.  If there are multiple matches
   * for a case-insensitive match, match case-sensitively, or yield an
   * error if that doesn't work.
   **/
  public static class EnumOpt<T extends Enum<T>> extends Opt<T> {

    /**
     * <p>Make a new EnumOpt with the given name and Enum type.
     **/
    public EnumOpt (OptSet _opts, String _name, Class<T> enumType)
    { super (_opts, _name, enumType); metavar ("ENUM"); } // XXX metavar

    /* Covariant builder methods: for convenient building. */
    public EnumOpt<T> defaultValue(T v) {super.defaultValue(v);return this;}
    public EnumOpt<T> name (String name) { super.name (name); return this; }
    public EnumOpt<T> shortName (char name){super.shortName(name);return this;}
    public EnumOpt<T> positional (boolean v){super.positional(v); return this;}
    public EnumOpt<T> required (boolean v) { super.required (v); return this; }
    public EnumOpt<T> list (boolean v) { super.list (v); return this; }
    public EnumOpt<T> metavar (String v) { super.metavar (v); return this; }
    public EnumOpt<T> description (String v){super.description(v);return this;}

    /**
     * <p>Parse the next token as an Enum value.
     **/
    protected T parseArg (OptReader in)
    {
      String value = in.read ();
      try {
        return Enum.valueOf (argType, value);
      } catch (IllegalArgumentException ex) {
        throw new OptParseException ("Bad value '"+value+"' for option "+this
                                     +": expected enum value.", ex);
      }
    }
  }

  /**
   * <p>A File option: parses filenames, and returns Files.
   *
   * <p>A FileOpt expects a single argument that is interpreted as a
   * filename on the host operating system.
   *
   * <p>Accepts filename arguments using either slash convention, and
   * converts them into system-standard conventions.
   *
   * <p>{@link #get(Cloptus.ParsedArgs)} returns objects of type
   * java.io.File
   *
   * <p>Provides a rich API to fetch the underlying file as a Stirng
   * pathname, File, Reader, Writer, InputStream, OutputStream, String
   * content, or byte[] content.
   *
   * <p>All Writers and Streams returned are buffered.
   **/
  public static class FileOpt extends Opt<File> {

    /**
     * <p>Make a FileOpt with the given name.
     **/
    public FileOpt (OptSet _opts, String _name)
    { super (_opts, _name, File.class); metavar ("FILE"); }

    /* Covariant builder methods: for convenient building. */
    public FileOpt defaultValue(File v) {super.defaultValue(v);return this;}
    public FileOpt name (String name) { super.name (name); return this; }
    public FileOpt shortName (char name){super.shortName(name);return this;}
    public FileOpt positional (boolean v){super.positional(v); return this;}
    public FileOpt required (boolean v) { super.required (v); return this; }
    public FileOpt list (boolean v) { super.list (v); return this; }
    public FileOpt metavar (String v) { super.metavar (v); return this; }
    public FileOpt description (String v){super.description(v);return this;}

    /**
     * <p>Fetches the filename of the specified file, normalized to
     * host OS conventions.
     *
     * <p>In particular, all slashes are normalized to local
     * conventions.
     **/
    public String getFileName (ParsedArgs parsedArgs)
    { return get (parsedArgs).getPath (); }

    /**
     * <p>Fetches the list of filenames specified.
     *
     * @see #getFileName(Cloptus.ParsedArgs)
     **/
    public List<String> getFileNameList (ParsedArgs parsedArgs)
    {
      List<String> r = new ArrayList<String> ();
      for (File f : getList (parsedArgs)) r.add (f.getPath ());
      return r;
    }

    /**
     * <p>Fetch the list of filenames specified as a Java String array.
     *
     * @see #getFileName(Cloptus.ParsedArgs)
     **/
    public String[] getFileNameArray (ParsedArgs parsedArgs)
    { return getFileNameList (parsedArgs).toArray (new String[0]); }

    /**
     * <p>Fetch a buffered InputStream on the contents of the specified file.
     **/
    public InputStream getInputStream (ParsedArgs parsedArgs)
      throws FileNotFoundException
    { return new BufferedInputStream (new FileInputStream (get(parsedArgs))); }

    /**
     * <p>Fetch a buffered OutputStream to write to the specified file.
     **/
    public OutputStream getOutputStream (ParsedArgs parsedArgs)
      throws FileNotFoundException
    {return new BufferedOutputStream (new FileOutputStream (get(parsedArgs)));}

    /**
     * <p>Fetch a buffered Reader on the contents of the specified file.
     *
     * <p>Uses the default system character set.
     **/
    public Reader getReader (ParsedArgs parsedArgs)
      throws FileNotFoundException
    { return new InputStreamReader (getInputStream (parsedArgs)); }

    /**
     * <p>Fetch a buffered Reader on the contents of the specified
     * file, using the named character set.
     **/
    public Reader getReader (ParsedArgs parsedArgs, String charsetName)
      throws FileNotFoundException, UnsupportedEncodingException
    { return new InputStreamReader (getInputStream (parsedArgs),charsetName); }

    /**
     * <p>Fetch a buffered Writer to write to the specified file.
     **/
    public Writer getWriter (ParsedArgs parsedArgs)
      throws FileNotFoundException
    { return new OutputStreamWriter (getOutputStream (parsedArgs)); }

    /**
     * <p>Fetch a buffered Writer to write to the specified file,
     * using the named character set.
     **/
    public Writer getWriter (ParsedArgs parsedArgs, String charsetName)
      throws FileNotFoundException, UnsupportedEncodingException
    {return new OutputStreamWriter (getOutputStream (parsedArgs),charsetName);}

    /**
     * <p>Fetch the raw contents of the specified file as an array of
     * bytes.
     **/
    public byte[] getByteContent (ParsedArgs parsedArgs) throws IOException
    { return IoLib.readFully (getInputStream (parsedArgs)); }

    /**
     * <p>Fetch the text content of the specified file as a String.
     *
     * <p>Uses the default system character set.
     **/
    public String getStringContent (ParsedArgs parsedArgs) throws IOException
    { return IoLib.readFully (getReader (parsedArgs)); }

    /**
     * <p>Fetch the text content of the specified file as a String,
     * using the named character set.
     **/
    public String getStringContent (ParsedArgs parsedArgs, String charsetName)
      throws IOException, UnsupportedEncodingException
    { return IoLib.readFully (getReader (parsedArgs, charsetName)); }

    /**
     * <p>Parses the next token as an abstract filename.
     *
     * <p>Normalizes slashes to environment-standard slashes.
     **/
    protected File parseArg (OptReader in)
    {
      String value = in.read ();

      // --- normalize newlines.
      value = value
        .replace ('/', File.separatorChar)
        .replace ('\\', File.separatorChar);;

      // --- make a java.io.File object
      return new File (value);
    }
  }

  /**
   * <p>An URI option: parses URIs.
   *
   * <p>An UriOpt expects a single argument that is interpreted as a
   * Uniform Resource Identifier.  If the argument appears to be a
   * filename, it is converted into the equivalent <tt>file:</tt> URI.
   *
   * <p>Accepts URIs with either slash convention, and normalizes them
   * into forward slashes (<tt>/</tt>).
   *
   * <p>{@link #get(Cloptus.ParsedArgs)} returns objects of type
   * java.net.URI
   *
   * <p>Provides a rich API to fetch the underlying resource as a
   * String URI, URL, URLConnection, Reader, InputStream, byte[]
   * content or String content.
   *
   * <p>All Readers and Streams are buffered.
   **/
  public static class UriOpt extends Opt<URI> {

    /**
     *
     **/
    public UriOpt (OptSet _opts, String _name)
    { super (_opts, _name, URI.class); metavar ("URI"); }

    /* Covariant builder methods: for convenient building. */
    public UriOpt defaultValue(URI v) {super.defaultValue(v);return this;}
    public UriOpt name (String name) { super.name (name); return this; }
    public UriOpt shortName (char name){super.shortName(name);return this;}
    public UriOpt positional (boolean v){super.positional(v); return this;}
    public UriOpt required (boolean v) { super.required (v); return this; }
    public UriOpt list (boolean v) { super.list (v); return this; }
    public UriOpt metavar (String v) { super.metavar (v); return this; }
    public UriOpt description (String v){super.description(v);return this;}

    /**
     * <p>Returns the String representation of an URI.
     *
     * <p>This may contain unquoted characters making it unsuitable
     * for using as a true URI, but is more pleasant to look at.
     *
     * @see URI#toString()
     **/
    public String getString (ParsedArgs parsedArgs)
    { return get (parsedArgs).toString (); }

    /**
     * <p>Returns the String representation of an URI.
     *
     * <p>This String representation contains only ASCII characters
     * (all non-ASCII characters are quoted).
     *
     * @see URI#toASCIIString()
     **/
    public String getASCIIString (ParsedArgs parsedArgs)
    { return get (parsedArgs).toASCIIString (); }

    /**
     * <p>Returns this URI object as an URL.
     *
     * @throws MalformedURLException under various circumstances (see
     * {@link URI#toURL()}), but mostly if the URI uses a scheme
     * unsupported by the Java runtime.
     **/
    public URL getURL (ParsedArgs parsedArgs) throws MalformedURLException
    {
      // XXX resolve relative to CWD
      return get (parsedArgs).toURL ();
    }

    /**
     * <p>Return a new URLConnection for this URL.
     *
     * @throws MalformedURLException under various circumstances (see
     * {@link URI#toURL()}), but mostly if the URI uses a scheme
     * unsupported by the Java runtime.
     *
     * @throws IOException if the URLConnection cannot be established.
     **/
    public URLConnection getURLConnection (ParsedArgs parsedArgs)
      throws MalformedURLException, IOException
    { return getURL (parsedArgs).openConnection (); }

    /**
     * <p>Return a new buffered Reader on the contents of the
     * specified URI.
     *
     * <p>Uses the default system character set.
     *
     * @throws MalformedURLException under various circumstances (see
     * {@link URI#toURL()}), but mostly if the URI uses a scheme
     * unsupported by the Java runtime.
     *
     * @throws IOException if the Reader cannot be created
     **/
    public Reader getReader (ParsedArgs parsedArgs)
      throws MalformedURLException, IOException
    { return new InputStreamReader (getInputStream (parsedArgs)); }

    /**
     * <p>Return a new buffered Reader on the contents of the
     * specified URI, using the named character set.
     *
     * @throws MalformedURLException under various circumstances (see
     * {@link URI#toURL()}), but mostly if the URI uses a scheme
     * unsupported by the Java runtime.
     *
     * @throws IOException if the Reader cannot be created
     *
     * @throws UnsupportedEncodingException if the named character set
     * is not supported.
     **/
    public Reader getReader (ParsedArgs parsedArgs, String charsetName)
      throws MalformedURLException, IOException, UnsupportedEncodingException
    { return new InputStreamReader (getInputStream (parsedArgs), charsetName);}

    /**
     * <p>Return a new buffered InputStream on the contents of the
     * specified URI.
     *
     * @throws MalformedURLException under various circumstances (see
     * {@link URI#toURL()}), but mostly if the URI uses a scheme
     * unsupported by the Java runtime.
     *
     * @throws IOException if the InputStream cannot be created
     **/
    public InputStream getInputStream (ParsedArgs parsedArgs)
      throws MalformedURLException, IOException
    { return new BufferedInputStream (getURL (parsedArgs).openStream ()); }

    /**
     * <p>Fetch the raw contents of the specified URI as an array of
     * bytes.
     *
     * @throws MalformedURLException under various circumstances (see
     * {@link URI#toURL()}), but mostly if the URI uses a scheme
     * unsupported by the Java runtime.
     *
     * @throws IOException if the the content of the resource cannot
     * be read.
     **/
    public byte[] getByteContent (ParsedArgs parsedArgs)
      throws MalformedURLException, IOException
    { return IoLib.readFully (getInputStream (parsedArgs)); }

    /**
     * <p>Fetch the content of the specified URI as a String.
     *
     * <p>Uses the system default character set.
     *
     * @throws MalformedURLException under various circumstances (see
     * {@link URI#toURL()}), but mostly if the URI uses a scheme
     * unsupported by the Java runtime.
     *
     * @throws IOException if the the content of the resource cannot
     * be read.
     **/
    public String getStringContent (ParsedArgs parsedArgs)
      throws MalformedURLException, IOException
    { return IoLib.readFully (getReader (parsedArgs)); }

    /**
     * <p>Fetch the content of the specified URI as a String, using
     * the named character set.
     *
     * @throws MalformedURLException under various circumstances (see
     * {@link URI#toURL()}), but mostly if the URI uses a scheme
     * unsupported by the Java runtime.
     *
     * @throws IOException if the the content of the resource cannot
     * be read.
     *
     * @throws UnsupportedEncodingException if the named character set
     * is not supported.
     **/
    public String getStringContent (ParsedArgs parsedArgs, String charsetName)
      throws MalformedURLException, IOException, UnsupportedEncodingException
    { return IoLib.readFully (getReader (parsedArgs, charsetName)); }

    /**
     * <p>Parses the next token as an URI
     *
     * <p>Deals with filenames, converts them into file: URIs.
     **/
    protected URI parseArg (OptReader in)
    {
      String value = in.read ();

      // --- this behavior matches what's found in Firefox/IE.

      // --- Win32 drive letter specification.
      if (value.matches ("[A-Za-z]:[\\\\/].*"))
        {
          // --- prepend 'file://' - for file: URI
          // ---                /  - for absolute URI
          value = "file:///" + value;
        }
      // --- Win32 UNC
      else if (value.matches ("[\\\\/][\\\\/].*"))
        {
          // --- prepend 'file://' - for file: URI
          // ---                /  - for absolute URI
          value = "file:///" + value;
        }
      // --- assume it's normal.
      else
        {}

      // --- convert all backslashes into forward slashes.
      value = value.replace ('\\', '/');

      // --- make a new URI object
      try {
        return new URI (value);
      } catch (URISyntaxException ex) {
        throw new OptParseException ("Bad value '"+value+"' for option "+this
                                     +": expected URI", ex);
      }
    }

  }

  // -------------------------------------------------------------------------
  // ---- ParsedArgs ---------------------------------------------------------
  // -------------------------------------------------------------------------

  /**
   * <p>A list of parsed options.  This is the result of ramming a
   * String[] of arguments into a call to {@link OptSet#run(String[])}
   * or {@link OptSet#parse(String[])}.
   *
   * <p>Various APIs can be used to fetch the results of parsing the
   * arguments.
   *
   * <p>{@link #getParsedArgs(Cloptus.Opt)} will fetch all of the
   * parsed options in the order that they were specified.  This is
   * handy for implementing complex programs like the UNIX command
   * 'find'.
   *
   * <p>Mostly, however, the typed getters on {@link Cloptus.Opt}
   * should be used.
   *
   * @see Cloptus.OptSet
   * @see Cloptus.Opt
   * @see Cloptus.OptSet#run(String[])
   * @see Cloptus.OptSet#parse(String[])
   **/
  public static class ParsedArgs implements Iterable<ParsedArg> {

    final OptSet opts;
    final String[] args;

    List<ParsedArg> parsedArgs = new ArrayList<ParsedArg> ();
    Map<Opt<?>,List<ParsedArg>> parsedArgsByOpt =
      new LinkedHashMap<Opt<?>,List<ParsedArg>> ();

    public ParsedArgs (OptSet _opts, String[] _args)
    { opts = _opts; args = _args; }

    void addOpts (Collection<ParsedArg> v)
    { for (ParsedArg pa : v) addOpt (pa); }

    void addOpt (ParsedArg v)
    {
      if (v == null) throw new NullPointerException ();
      parsedArgs.add (v);

      List<ParsedArg> existingParsedArgs = parsedArgsByOpt.get (v.opt);
      if (existingParsedArgs == null)
        {
          existingParsedArgs = new ArrayList<ParsedArg> ();
          parsedArgsByOpt.put (v.opt, existingParsedArgs);
        }
      existingParsedArgs.add (v);
    }

    void validate ()
    {
      if (DebugValidate)
        System.err.println ("validate parsedArgs == "+parsedArgs);

      // --- validate 'required' feature.
      for (Opt<?> a : opts.getOpts ())
        if (a.isRequired ())
          if (parsedArgsByOpt.get (a) == null)
            throw new OptParseException ("Missing required option "+a);

      // --- validate 'list' feature.
      for (Opt<?> a : getInvolvedOpts ())
        {
          List<ParsedArg> parsedArgsForOpt = getParsedArgs (a);

          if (!a.isList ())
            if (parsedArgsForOpt.size () > 1)
              throw new OptParseException
                (a+" can only be specified once, was specified "+
                 parsedArgsForOpt.size ()+" times.");
        }
    }

    // ----------------------------------------------------------------------

    /**
     *
     **/
    public void populateMembers (Object targetObj)
    {for (Opt<?> opt : opts.getOpts ()) opt.populateMember (targetObj, this);}

    // ----------------------------------------------------------------------

    /**
     * Iterate over the result of {@link #getParsedArgs()}.
     **/
    public Iterator<ParsedArg> iterator () { return parsedArgs.iterator (); }

    /**
     * Retrieve the parsed args specified by the user.
     **/
    public List<ParsedArg> getParsedArgs () { return parsedArgs; }

//     /**
//      * Retrieve the parsed args specified by the user, followed by
//      * synthesized parsed args for unspecified options with default
//      * values.
//      **/
//     public List<ParsedArg> getParsedArgsWithDefaults ()
//     {
//       // XXX should we cache this??
//       List<ParsedArg> r = new ArrayList<ParsedArg> ();

//       // --- add all the regular parsed args.
//       r.addAll (parsedArgs);

//       // --- figure out the unspecified args with default values, add them too.
//       for (Opt<?> opt : opts.getOpts ())
//         if (getValues (opt, opt.getArgType (), null).size () == 0
//             && opt.defaultValue != null)
//           r.add (new ParsedArg (opt, opt.getName (), opt.defaultValue, true));

//       return r;
//     }

    /**
     * <p>Retrieve the set of Opts that have some arg specified in this
     * ParsedArgs set.
     **/
    public Set<Opt<?>> getInvolvedOpts () { return parsedArgsByOpt.keySet (); }

    /**
     * <p>Retrieve the parsed args specified for the given Opt.
     **/
    public List<ParsedArg> getParsedArgs (Opt<?> opt)
    {
      List<ParsedArg> r = parsedArgsByOpt.get (opt);
      if (r == null)
        { r = new ArrayList<ParsedArg>(); parsedArgsByOpt.put (opt, r); }
      return r;
    }

    /**
     * <p>Retrieve the value for the given Opt instance contained in
     * these ParsedArgs.
     *
     * <p>If the option was not specified, return the given default
     * value.
     *
     * @throws OptUseException if the given Opt is a list, and was
     * specified more than once,
     **/
    public <T> T getValue (Opt<T> opt, Class<T> type, T defaultValue)
    {
      List<T> r = getValues (opt, type, defaultValue);
      switch (r.size ())
        {
        case 0: return null;
        case 1: return r.get (0);
        default:
          throw new OptUseException
            ("Asked for singleton value of opt "+opt+", got list");
        }
    }

    /**
     * <p>Retrieve the values for the given Opt instance contained in
     * these ParsedArgs.
     *
     * <p>If the option was not specified, return a list containing
     * the given default value.
     **/
    public <T> List<T> getValues (Opt<T> opt, Class<T> type, T defaultValue)
    {
      List<ParsedArg> parsedArgs = getParsedArgs (opt);
      List<T> r = new ArrayList<T> ();
      for (ParsedArg pa : parsedArgs) r.add (pa.getValue (type));

      // --- default value handling.
      if (r.size () == 0 && defaultValue != null) r.add (defaultValue);

      return r;
    }

    /**
     * <p>Return true if the magic --help option was specified.
     **/
    public boolean isHelpRequested ()
    { return opts.getHelpOpt ().getBoolean (this); }

    /**
     * <p>Make a nice string rendering of this ParsedArgs set.
     **/
    public String toString ()
    {
      StringBuilder out = new StringBuilder ();
      boolean first = true;
      for (ParsedArg opt : parsedArgs)
        {
          if (first) first = false;
          else       out.append (" ");
          out.append (opt);
        }
      return out.toString ();
    }
  }

  /**
   * <p>A single ParsedArg.  This represents a user-specified argument.
   *
   * <p>Contains the Opt matched against, the name specified by the
   * user (null for a positional argument), and the Object value that
   * was derived.
   **/
  public static class ParsedArg {
    final Opt<?> opt;
    final String nameSeen;
    final Object value;
    final boolean synthesized;

    public ParsedArg (Opt<?> _opt, String _nameSeen, Object _value,
                      boolean _synthesized)
    {
      opt = _opt; nameSeen = _nameSeen; value = _value;
      synthesized = _synthesized;
    }

    public Opt<?> getOpt () { return opt; }

    public String getNameSeen () { return nameSeen; }

    public <T> T getValue (Class<T> type) { return type.cast (value); }

    public String toString () { return "--" + opt.getName () + "=" + value; }
  }

  // -------------------------------------------------------------------------
  // ---- Exceptions ---------------------------------------------------------
  // -------------------------------------------------------------------------

  /**
   * <p>Base type for option exceptions.
   **/
  public abstract static class OptException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    public OptException () { super (); }
    public OptException (String msg) { super (msg); }
    public OptException (String msg, Throwable ex)
    { super (msg); initCause (ex); }
  }

  /**
   * <p>Thrown if there's a problem with the specification of an option.
   **/
  public static class OptCompileException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    public OptCompileException () { super (); }
    public OptCompileException (String msg) { super (msg); }
    public OptCompileException (String msg, Throwable ex) { super (msg, ex); }
  }

  /**
   * <p>Thrown if there's a problem during the parsing of an option.
   **/
  public static class OptParseException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    public OptParseException () { super (); }
    public OptParseException (String msg) { super (msg); }
    public OptParseException (String msg, Throwable ex) { super (msg, ex); }
  }

  /**
   * <p>Thrown if there's a problem using a ParsedArgs set.
   **/
  public static class OptUseException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    public OptUseException () { super (); }
    public OptUseException (String msg) { super (msg); }
    public OptUseException (String msg, Throwable ex) { super (msg, ex); }
  }

  // -------------------------------------------------------------------------
  // ---- Utility Classes ----------------------------------------------------
  // -------------------------------------------------------------------------

  /**
   * <p>Reads args.
   **/
  static class OptReader {
    private String[] args;
    private int position = 0;

    public OptReader (String[] _args) { args = _args; }

    public String read ()
    {
      if (position >= args.length) return null;
      return args[position++];
    }

    public String peek ()
    {
      if (position >= args.length) return null;
      return args[position];
    }

    public void prependArg (String arg)
    { prependArgs (new String[] { arg }); }

    public void prependArgs (String[] v)
    {
      // --- try to stick the new args into our current array
      // XXX disabled for debugging of the more complex second part.
//      if (v.length < position)
//        {
//          System.arraycopy (v, 0,
//                            args, position-v.length,
//                            v.length);
//          position -= args.length;
//        }

      // --- elsewise, make a new array.
//      else
        {
          String[] newArgs = new String[v.length + (args.length - position)];

          System.arraycopy (v, 0,
                            newArgs, 0,
                            v.length);
          System.arraycopy (args, position,
                            newArgs, v.length,
                            (args.length - position));

          args = newArgs;
          position = 0;
        }
    }
  }

  /**
   *
   **/
  public static class StringPrintWriter extends PrintWriter
  {
    public StringPrintWriter () { super (new StringWriter ()); }
    public String toString () { flush (); return out.toString (); }
  }

  /**
   *
   **/
  public static class WordWrap {

    /**
     *
     **/
    public static void wordwrap (PrintWriter out, String text)
    { wordwrap (out, text, "    ", true); }

    /**
     *
     **/
    public static void wordwrap (PrintWriter out, String text,
                                 String IndentSpaces, boolean indentFirstLine)
    {
      if (text == null) return;

      int col = 0;
      String[] words = text.split (" ");

      if (indentFirstLine) out.print (IndentSpaces);

      int lineLength = 78 - IndentSpaces.length ();

      for (String word : words)
        {
          int wordLength = word.length ();
          int spaceLeft = lineLength - col;

          if (wordLength >= spaceLeft)
            {
              out.println ();
              out.print (IndentSpaces);
              col = 0;
            }

          out.print (" ");
          out.print (word);
          col += word.length () + 1;
        }
    }

  }

  /**
   *
   **/
  public static class NumericUtil {

    static Map<String,Long> ComputerMultipliers = new HashMap<String,Long> ();
    static Map<String,Long> StandardMultipliers = new HashMap<String,Long> ();

    static {
      // --- kilo, kibi
      ComputerMultipliers.put ("K", 1024L);
      StandardMultipliers.put ("K", 1000L);
      ComputerMultipliers.put ("Ki", 1024L);
      StandardMultipliers.put ("Ki", 1024L);

      // --- mega, mebi
      ComputerMultipliers.put ("M", 1048576L);
      StandardMultipliers.put ("M", 1000000L);
      ComputerMultipliers.put ("Mi", 1048576L);
      StandardMultipliers.put ("Mi", 1048576L);

      // --- giga, gibi
      ComputerMultipliers.put ("G", 1073741824L);
      StandardMultipliers.put ("G", 1000000000L);
      ComputerMultipliers.put ("Gi", 1073741824L);
      StandardMultipliers.put ("Gi", 1073741824L);

      // --- tera, tebi
      ComputerMultipliers.put ("T", 1099511627776L);
      StandardMultipliers.put ("T", 1000000000000L);
      ComputerMultipliers.put ("Ti", 1099511627776L);
      StandardMultipliers.put ("Ti", 1099511627776L);

      // --- peta, pebi
      ComputerMultipliers.put ("P", 1125899906842624L);
      StandardMultipliers.put ("P", 1000000000000000L);
      ComputerMultipliers.put ("Pi", 1125899906842624L);
      StandardMultipliers.put ("Pi", 1125899906842624L);

      // --- exa, exbi
      ComputerMultipliers.put ("E", 1152921504606846976L);
      StandardMultipliers.put ("E", 1000000000000000000L);
      ComputerMultipliers.put ("Ei", 1152921504606846976L);
      StandardMultipliers.put ("Ei", 1152921504606846976L);
    }

    public static String getSuffix (String numberStr)
    {
      int i=0;
      for (; i<numberStr.length (); i++)
        if (!Character.isDigit (numberStr.charAt (i))) break;
      if (i == numberStr.length ()) return null;
      return numberStr.substring (i);
    }

    /**
     *
     **/
    public static long getMultiplier (String suffix) { return 1; }
  }

  /**
   * <p>Does I/O primitives.
   **/
  public static class IoLib {

    /**
     *
     **/
    public static byte[] readFully (InputStream in) throws IOException
    {
      ByteArrayOutputStream out = new ByteArrayOutputStream ();
      copy (in, out);
      return out.toByteArray ();
    }

    /**
     *
     **/
    public static String readFully (Reader in) throws IOException
    {
      StringWriter out = new StringWriter ();
      copy (in, out);
      return out.toString ();
    }

    public static int copy (InputStream in, OutputStream out)
      throws IOException
    { return copy (in, out, 4096); }
    public static int copy (InputStream in, OutputStream out, int bufSize)
      throws IOException
    {
      int copied = 0;
      byte[] buf = new byte[bufSize];
      int bytesRead;
      while ((bytesRead = in.read (buf)) != -1)
	{
	  out.write (buf, 0, bytesRead);
	  copied += bytesRead;
	}
      return copied;
    }

    public static int copy (Reader in, Writer out) throws IOException
    { return copy (in, out, 4096); }
    public static int copy (Reader in, Writer out, int bufSize)
      throws IOException
    {
      int copied = 0;
      char[] buf = new char[bufSize];
      int charsRead;
      while ((charsRead = in.read (buf)) != -1)
	{
	  out.write (buf, 0, charsRead);
	  copied += charsRead;
	}
      return copied;
    }
  }

}


/*
Notes:

- need 3 annotations:

   1. flag a field (or method) of an object as an option target

   2. flag an Opt subtype as being the 'default' Opt subtype for a
      particular type. (String: StringOpt, etc)

   3. flag methods on Opt as returning different types (so we can have
      a String read from a FileOpt, for example)

- 3 is irritating.

- sometimes there is more than one interpretation of a type/opt pair
  (String could mean the file name or the file contents).

- we could leave this feature out.  That way, if you read a FileOpt, 
  you couldn't get the nice options.  But this is irritating.

- we could specify the method using a String - this is the most
  straightforward, but kindof irritating.

 - we could wrap options in other objects to get this information, but
   this is kindof wierd.





00        10        20        30        40        50        60        70
.123456789.123456789.123456789.123456789.123456789.123456789.123456789.12345678

--foo=STRING, -f STRING       Foo is cool.  It really rocks.


SO: algorithm is:
   - print args, comma seperated.
   - if result is > col 30, print newline + 30 spaces.
   - otherwise, advance to column 30 with spaces.

   - do word-wrap (78 chars), with left indent of 30 characters.



GNU GetOpt example output:

  -a, --all                  do not hide entries starting with .
  -A, --almost-all           do not list implied . and ..
  -b, --escape               print octal escapes for nongraphic characters
      --block-size=SIZE      use SIZE-byte blocks
  -B, --ignore-backups       do not list implied entries ending with ~
  -c                         with -lt: sort by, and show, ctime (time of last
                               modification of file status information)
                               with -l: show ctime and sort by name
                               otherwise: sort by ctime
  -C                         list entries by columns
      --color[=WHEN]         control whether color is used to distinguish file
                               types.  WHEN may be `never', `always', or `auto'
  -d, --directory            list directory entries instead of contents
  -D, --dired                generate output designed for Emacs' dired mode
  -f                         do not sort, enable -aU, disable -lst

*/
