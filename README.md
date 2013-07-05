# drplate

A Clojure library designed to template files

## Usage

### API
- core.init [], initializes the library version number if you need it inside a template.
- core.insertVar [^String key value], creates a new binding available in the template env.
- core.templateFile [^String template-file ^String result-file ^boolean double-pass], expands the
  macros inside the template file, and write the result to result-file.

### Template :
- When expanding a template file regular text is left unmodified and macros are expanded to the
  result file.
- Macros are all text enclosed by `{% %}`
- Macros content must be valid clojure code. If a macro code returns a string, the macro will
  expand to this string. Otherwise the result will be printed to the screen. Any global definition
  made in one macro is available in subsequent macros
## License

Copyright © 2013 Raphael Catolino

Distributed under the MIT public license.
