# JTraits
My go at bringing scala traits to Java 6.
Should be mostly working, with issues in some edge cases which can usually be fixed by setting the value to a variable and reading off that variable instead.

## What are Traits/JTraits?
Traits are a fundamental part of the Scala language which allow you to add methods with and without bodies and which can be applied to classes much like interfaces. What this allows you to do is to implement common things into a trait and add it to all the classes you want that code in. That way, the code is shared between all of those classes instead of being copied over and over again. JTraits allows you to do that inside Java using the magic of ASM.

## Currently supported features
 * Creation of traits: Traits are classes which can be mixed into a class or another mixin.
 * Method and variable supercalls: You can call methods in a trait's mixed-in parent and access its variables.
 * Constructor forwarding: Constructors will be built matching the ones inside the trait. A call will be made to the parent constructor with the passed arguments and then the trait's constructor will be ran.
 * Easy stack tracking: Mixins will forward the original line numbers from the trait along with a reference to it, allowing IDEs to track the source of the mixin down easily when the stack gets dumped (for example, when a crash gets printed to the console), allowing you to easily find that code.