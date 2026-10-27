# rubytext-zh notices

The code in this artifact is under the Apache License 2.0, like the rest of
RubyText. The data files it ships are not:

| What | File | License |
| --- | --- | --- |
| Taiwan reading table, built from McBopomofo, libtabe, CC-CEDICT and Unihan | `zh-tw.txt.gz` | CC BY-SA 4.0 (inherits the MIT, BSD-3-Clause and Unicode notices below) |
| Mainland reading table, built from CC-CEDICT, Unihan and McBopomofo word counts | `zh-cn.txt.gz` | CC BY-SA 4.0 (inherits the MIT and Unicode notices below) |
| Simplified/Traditional character sets, built from Unihan | `zh-variants.json.gz` | Unicode License v3 |

The files are copied unchanged (then gzipped) from
[Ruby Everywhere](https://github.com/b84330808/ruby-everywhere) `data/`, commit
`b5fdd84f11edfb6607e95bf59e1b2819bd9181ed`; `SOURCES.json` records the upstream
versions. If you redistribute these tables or something adapted from them, the
CC BY-SA 4.0 terms (attribution, share-alike) apply to that data. Using the
artifact in an app does not change the license of your own code.

## Chinese reading tables

These two files are adapted from CC-CEDICT, so they are distributed under
the [Creative Commons Attribution-ShareAlike 4.0 International
License](https://creativecommons.org/licenses/by-sa/4.0/). You may share
and adapt them under the same license, with attribution to the sources
below. Changes made: readings converted to Zhuyin, one reading chosen per
word, words read with their characters' usual readings reduced to a
marker, word counts turned into rounded log frequencies. The scripts that
build them are `scripts/fetch-sources.mjs` and `scripts/build-data.mjs` in Ruby Everywhere.

The MIT, BSD and Unicode notices below still apply to the parts of these
files that come from those projects.

### CC-CEDICT

> CC-CEDICT, community maintained free Chinese-English dictionary,
> published by MDBG. <https://www.mdbg.net/chinese/dictionary?page=cc-cedict>
> Licensed under [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/).
> Referenced works: CEDICT, Copyright (C) 1997, 1998 Paul Andrew Denisowski.

Used for: mainland readings, Simplified forms, Taiwan readings marked
"Taiwan pr.", and choosing between alternative readings.

### McBopomofo (小麥注音輸入法) data

Files used: `Source/Data/BPMFBase.txt`, `BPMFMappings.txt`, `phrase.occ`,
`heterophony1.list`, `heterophony2.list`, `heterophony3.list` from
<https://github.com/openvanilla/McBopomofo>.

```
MIT License

Copyright (c) 2011-2026 Mengjuei Hsieh et al.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

### libtabe `tsi.src`

McBopomofo's `BPMFMappings.txt` was originally derived from `tsi.src` of
libtabe (<https://github.com/kcwu/libtabe>). Its `COPYING` file, with two
BSD-style licenses (Copyright (c) 1999 TaBE Project, Copyright (c) 1999
Pai-Hsiang Hsiao; Copyright (c) 1999 Computer Systems and Communication
Lab, Institute of Information Science, Academia Sinica) and the notice
"Copyright 1996 Chih-Hao Tsai @ Beckman Institute, University of
Illinois", is reproduced unchanged in `licenses/libtabe-tsi-COPYING.txt`.

### Unihan database

Fields used: `kMandarin` (default character readings) from
`Unihan_Readings.txt`; `kSimplifiedVariant` and `kTraditionalVariant`
from `Unihan_Variants.txt`. Unicode Character Database, © 1991-2026
Unicode, Inc., under the Unicode License v3; the full text is in
`licenses/Unicode-3.0.txt`.

