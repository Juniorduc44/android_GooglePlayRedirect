# CustomTkinter notes (for this project)

Official docs: https://customtkinter.tomschimansky.com/

Fetched/summarized for widgets used in php-usd-converter + Translator.

## Theme (app-wide)

```python
import customtkinter as ctk
ctk.set_appearance_mode("Dark")       # "System" | "Dark" | "Light"
ctk.set_default_color_theme("blue")   # "blue" | "green" | "dark-blue"
```

## CTkTabview

Docs: https://customtkinter.tomschimansky.com/documentation/widgets/tabview/

- `.add("Name")` creates a tab (returns a `CTkFrame`-like surface).
- `.tab("Name")` returns the tab frame for packing children.
- `.set("Name")` / `.get()` select / query visible tab.
- Tabs are frames: pack/grid any widgets on them.

```python
tabs = ctk.CTkTabview(master)
tabs.pack(fill="both", expand=True)
tabs.add("Convert")
frame = tabs.tab("Convert")
ctk.CTkButton(frame, text="Go").pack()
```

## CTkTextbox

Docs: https://customtkinter.tomschimansky.com/documentation/widgets/textbox/

- Indices like tkinter Text: `"0.0"`, `"end"`.
- `insert("0.0", text)`, `get("0.0", "end")`, `delete("0.0", "end")`.
- `configure(state="disabled")` for read-only.

## CTkOptionMenu

Docs: https://customtkinter.tomschimansky.com/documentation/widgets/optionmenu/

```python
menu = ctk.CTkOptionMenu(master, values=["a", "b"], command=on_pick)
menu.set("a")
choice = menu.get()
menu.configure(values=["x", "y"])
```

## Other widgets used here

| Widget | Use |
|---|---|
| `CTk` | Root window |
| `CTkFrame` | Cards / rows |
| `CTkScrollableFrame` | Travel (long content) |
| `CTkLabel` | Labels / results |
| `CTkEntry` | Single-line amounts |
| `CTkButton` | Actions / adjacent switches |
| `CTkRadioButton` | Settings text size |
| `CTkSwitch` | (available; we use compact buttons for unit switches) |

## Layout tips (this app)

- Dark cards: `fg_color="#1E293B"` for result panels.
- Adjacent control: horizontal `CTkFrame` with label `side="left"` and switch button `side="right"`.
- Long AI calls: run in a background `threading.Thread`, update widgets with `app.after(0, callback)` so the UI stays responsive.
