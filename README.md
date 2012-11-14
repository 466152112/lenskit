# LensKit

LensKit is an implementation of collaborative filtering algorithms and
a set of tools for benchmarking them.  This readme is about working with
the LensKit source code.  For more information about
LensKit and its documentation, visit the [web site][] or [wiki][].  You 
can also find information on the [wiki][] about how to use LensKit 
without downloading the source code.

[web site]: http://lenskit.grouplens.org
[wiki]: http://dev.grouplens.org/trac/lenskit


## Installation and Dependency Management

LensKit is built and deployed via [Maven][].  To install it, check out
this repository and run `mvn install`; it is then available to other projects by
depending directly on it as above (consult `pom.xml` for the version to use; all LensKit
modules share the same version).  The source code can also be checked out and used
in most Java IDEs; NetBeans in IntelliJ both include Maven support, and the [m2e][]
plugin for Eclipse (installable from the Eclipse Marketplace) allows the project to
be opened in Eclipse. (More details on using Eclipse with LensKit are below.)

[Maven]: http://maven.apache.org
[m2e]: http://eclipse.org/m2e

## Modules

LensKit is comprised of several modules.  The top-level `lenskit`
module serves as a container to build them and provide common settings
and dependencies.  The other modules are as follows:

* `lenskit-api` -- the common, public recommender API exposed by LensKit, independent
  of its actual implementations.
* `lenskit-data-structures` -- common data structures used by LensKit.
  These are split from `-core` so the API can depend on them.
* `build-tools` -- configuration & support files for building LensKit
* `lenskit-core` -- the core support code and configuration facilities for
  the rest of LensKit. It is the entry point for most of what you want to do with
  LensKit, providing support for configuring and building recommenders.
* `lenskit-knn` -- k-NN recommenders (user-user and item-item collaborative
  filtering).
* `lenskit-svd` -- the FunkSVD recommender (and eventually real SVD recommenders).
* `lenskit-slopeone` -- Slope-One recommenders.
* `lenskit-eval` -- the evaluation framework and APIs, along with a command line
  evaluation runner.
* `lenskit-eval-demo` -- a set of example scripts for configuring and running LensKit
  offline evaluations.
* `lenskit-package` -- a metapackage for preparing binary distributions, including
  scripts for running the evaluator.


## Notes for Eclipse

If you want to work with the LensKit sources in Eclipse, the checkout is complicated a bit
by limitations in M2e's integration with Mercurial. You'll need the Maven support ([m2e][]),
[MercurialEclipse][HGE] plugin, and the Groovy development tools (at least to work with the
evaluator code).  

First: do Help / Check for Updates and install any updates that are available.  We have had
problems in which the tool versions were not compatible with the Eclipse version, and those
problems can be frustrating to debug.  To get the tools:

* Maven.  Do Help / About / Installation Details / Features and check if m2e is already installed.
  It comes with many modern eclipse distros.  If not use Help / Marketplace, and search for Maven
  or m2e.
* Mercurial: Help / Marketplace and search for MercurialEclipse.  Install the option for 
  your version of Eclipse.
* Groovy: Help / Marketplace and search for Groovy.  It's fine to install the full version 
  including Grails from springsource, but all you need is Groovy-Eclipse.
 
Then do the following:

* Check out the LensKit source tree by selecting File / Import / "Clone existing repository".
  Enter the URL and authentication information, and clone the repository into your workspace.
* Once the project is imported, right-click it and select "Configure" -> "Convert to Maven project"
  to activate Maven support on the LensKit parent project.
* Finally, right-click the `lenskit` project, choose "Import", and select "Maven" / "Existing
  Maven Projects". Choose all sub-projects of `lenskit` and import them.

The `lenskit-eval` project requires some additional attention to compile correctly in Eclipse:

* First, right-click the `lenskit-eval` project and select "Configure" -> "Convert to 
  Groovy project" to enable Groovy support.
* Next, right-click the `lenskit-eval` project and select "Properties" / "Build Path" /
  "Link Source".
* In the window that appears, select the "Browse..." button and browse to the directory that
  contains the `lenskit` project. Then, select "lenskit-eval" / "src" / "main" / "groovy".
  You may change the suggested folder name if you wish, then click "finish".
* Finally, click the "Link Source..." button a second time and browse to "lenskit-eval" / 
  "src" / "test" / "groovy". Make sure that this folder has a different name than the first
  Groovy folder, and click "Finish" -> "OK".  
  
You will now have all components of LensKit imported and ready to edit and compile in Eclipse.

You can run Maven from Eclipse by right clicking on one of the pom.xml files, and selecting
"Build".  You can choose the goal you want to run, and it should directly run.  Note that you
will probably get the error message:

SLF4J: Failed to load class "org.slf4j.impl.StaticLoggerBinder".
SLF4J: Defaulting to no-operation (NOP) logger implementation
SLF4J: See http://www.slf4j.org/codes.html#StaticLoggerBinder for further details.

while appears to be harmless.

A useful thing to do first is to right click on the build-tools project and select
"Run As" / "Maven Install".  That will install the build tools in your local Maven
repository, so they are available for any of the other projects to use.

After that, you can easily run tests in any of the other projects directly from Eclipse,
which is faster and prettier.  Just right click a project, select "Run As" / "JUnit Test".

Mercurial on Windows is sometimes a little tricky to get set up.  If you do not have a Mercurial.ini
file, Mercurial will not know the user name to use for commits.  Here is a stackoverflow Q&A that 
explains how to set it up:

    http://stackoverflow.com/questions/2329023/mercurial-error-abort-no-username-supplied

You will need to install a Java Development Kit to build some parts of LensKit.  On Linux or Mac, setting
the Java HOME will let LensKit find it. On Windows if you install the JDK next to the JRE,
LensKit will probably be able to find it.

At the bottom of the LensKit Code Guidelines (http://dev.grouplens.org/trac/lenskit/wiki/CodeGuidelines) there is 
an XML file that will set your Eclipse up to follow the LensKit style.
