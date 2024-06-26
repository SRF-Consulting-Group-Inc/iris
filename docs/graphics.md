# Graphic Images

Select `View ➔ Message Signs ➔ Graphics` menu item

Graphic images can be displayed on [DMS] [sign message]s.  These can be warning
pictographs, interstate shields or other signs.

<details>
<summary>API Resources 🕵️ </summary>

* `iris/api/graphic` (primary)
* `iris/gif/{name}.gif`

| Access  | Primary      |
|---------|--------------|
| 👁️  View | name, number |

</details>

Image editing, resizing, painting, *etc.* is not supported - graphics must be
edited and sized properly before importing into the IRIS image library.  Use an
image editor such as [GIMP] or Microsoft Paint for this purpose.

Requirements:

- Image file name: `G`{graphic number}`.gif`
- 1-bit monochrome or 24-bit color
- Width between 1 and 240 pixels
- Height between 1 and 144 pixels

Store graphics in `/var/lib/iris/web/gif/` to make available in the [REST API].

To import a graphic into the database, enable [edit mode] and press the
**Create** button.  Select the image in the file picker.

A single **Transparent Color** can be selected to be fully transparent - alpha
transparency is not supported. 

Each image has a unique **Graphic Number**, between 1 and 999.  If the number
is below 256, it can be used in a [MULTI] `[g`...`]` tag to display the graphic
on a sign message.

A graphic can also be used to represent a [static sign] with a smaller DMS
inset, if the graphic number is 256 or greater.  For example, a static sign
might contain a travel time message with a fixed destination, and the DMS inset
would only be large enough to display a 2-digit number of minutes.  The static
graphic is displayed when selecting the DMS, to help operators understand the
sign's function.


[DMS]: dms.html
[edit mode]: ui.html#edit-mode
[GIMP]: https://www.gimp.org/
[MULTI]: multi.html
[REST API]: rest_api.html
[sign message]: sign_message.html
[static sign]: dms.html#setup
