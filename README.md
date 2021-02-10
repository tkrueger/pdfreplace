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

No fancy magic in here, just plain old regexes. On the other side, the `replacements` argument is read and interpreted by clojure, so some hacking may be possible, if required.

Also, PDF is complicated, and there's no guarantee that the text you're locking for doesn't have some placement token in between. So, if your regex doesn't catch what you want to change, try to look for smaller texts. The tool will only miss texts that span multiple COSArrays. This is done by sifting out character placement information in between texts inside a COSArray. This lose a bit of layout, which probably wouldn't fit your replacement anyway. The results were nice enough for me.

Bear in mind that this has been created for a specific set of generated PDFs. I haven't spent much time investigating others, but there seem to be a lot of ways to spread text across a pdf, and other sources may not find text to replace. The approach might still help you on your way though.
