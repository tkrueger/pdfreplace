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

### Matching Text

Also, PDF is complicated, and there's no guarantee that the text you're loocking for doesn't have some placement token in between. So, if your regex doesn't catch what you want to change, try to look for smaller texts. The tool will only miss texts that span multiple COSArrays. This is done by sifting out character placement information in between texts inside a COSArray. This loses a bit of layout, which probably wouldn't fit your replacement anyway. The results were nice enough for me.

Bear in mind that this has been created for a specific set of generated PDFs. I haven't spent much time investigating others, but there are a lot of ways to spread text across a pdf, and other sources may not find text to replace. The approach might still help you on your way though.

### Type 3 Fonts

The tool can only encode what pdfbox can encode. As of now, that isn't true for Type3 fonts, for example. In these cases, a replacement font is inserted into the document and used instead. This changes the optics, but fits the use case of building anonymized documents save to check in.

### Small Font Sizes

If you encounter a rather empty PDF after replacing, one possible cause is that the document uses a font that works with very small sizes, e.g. 1 or 2. This can produce invisible text in the replacement font (see above). As a fix, a minimum font size is used, which can be affected using the command line options.
