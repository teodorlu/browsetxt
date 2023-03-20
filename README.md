# browsetxt has MOVED!

New home here: https://github.com/teodorlu/shed/tree/b2c402b8afa4fdaf422a88b4d5170fda2d469479/contrib/browsetxt/README.md

---

# browsetxt

Hypertext as plaintext.

## Prerequiesites

Please install [babashka][babashka], [babashka/bbin][bbin], [fzf][fzf] and [Pandoc][pandoc].

[babashka]: https://babashka.org/
[bbin]: https://github.com/babashka/bbin
[fzf]: https://github.com/junegunn/fzf
[pandoc]: https://pandoc.org/

## Installing

In a terminal, run

    bbin install io.github.teodorlu/browsetxt --latest-sha

## Usage

In a terminal, run 

    browsetxt URL

For example:

    browsetxt https://en.wikipedia.org/wiki/Krakatoa

## Markdown syntax highlighting

It's possible to get colors in your document rendering by relying on [bat][bat].
Install `bat`, then use the `--bat-mardown` CLI argument:

    browsetxt https://en.wikipedia.org/wiki/Krakatoa --bat-markdown

[bat]: https://github.com/sharkdp/bat
