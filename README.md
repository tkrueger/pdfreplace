# pdfreplace

A tiny tool able to replace text in pdf documents.

## Use Case

This was born out of a desire to anonymize documents in order to use them as test files in another project. 

All it can do is copy a pdf file from source to target, and replacing all occurrences of given regexes with replacement strings.

## Usage from the Command Line

After cloning, either use 

```bash
clj -Mmain
```

or build an uberjar and use that:

```bash
clj -Spom
clj -X:depstar uberjar :jar pdfreplace.jar :aot true :main-class pdfreplace.main
java -jar pdfreplace.jar
``` 

Unfortunately, it looks like graalvm doesn't like pdfbox used in [pdfboxing](https://github.com/dotemacs/pdfboxing) referencing AWT classes. At least, building a native image didn't work.

## Caveats

No fancy machine learning in here, just plain old regexes. On the other side, the `replacements` argument is read and interpreted by clojure, so some hacking may be possible, if required.

Also, PDF is weird, and there's no guarantee that the text you're loocking for doesn't have some placement token in between. So, if your regex doesn't catch what you want to change, try to look for smaller texts. The tool doesn't try to combine texts by filtering out placements. This would be easy to do. I didn't need it for my use case, though, so didn't implement it.

Bear in mind that this has been created for a specific set of generated PDFs. I haven't spent much time investigating others, but there seem to be a lot of ways to spread text across a pdf, and other sources may not work. The approach might still help you on your way though.