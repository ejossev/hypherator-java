Hypherator - Java Library
A lightweight, MIT-licensed hyphenation library for Java, based on Hunspell-style hyphenation patterns. It provides a simple API to hyphenate words using an iterator pattern, making it easy to integrate into tokenizer-based workflows.

Features:
Hunspell-style hyphenation rules

Lightweight and pure Java (no native code or JNI)

Iterator-based API for compatibility with existing tokenizer designs (e.g., ICU4J)

Open-source under the MIT license

Usage:
```
Hypherator hypherator = new Hypherator("en_US");
String word = "typography";
HyphenIterator iterator = hypherator.hyphenate(word);

while (iterator.hasNext()) {
    System.out.print(iterator.next());
}

```

License:
MIT License. See LICENSE file for more information.

Compatible LibreOffice hyphenation dictionaries are bundled directly, so you can use Hypherator out of the box for most languages — no additional setup required.

Sponsored by [pdf365.cloud](https://pdf365.cloud).