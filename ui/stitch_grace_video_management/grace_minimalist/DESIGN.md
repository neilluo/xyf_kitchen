# Design System Strategy: The Culinary Editorial

## 1. Overview & Creative North Star
This design system is built on the Creative North Star of **"The Digital Curator."** For a platform like Grace, which serves food bloggers and non-technical creators, the UI must move beyond "SaaS utility" and into the realm of a "High-End Editorial." 

We reject the rigid, boxed-in look of standard dashboards. Instead, we embrace a layout that feels like a premium digital magazine: expansive, airy, and layered. By utilizing intentional asymmetry, overlapping elements, and a sophisticated tonal hierarchy, we transform video distribution from a technical chore into a curated experience. We use depth and soft transitions to guide the eye, ensuring the user feels "held" by the interface rather than overwhelmed by it.

---

## 2. Colors & Tonal Architecture
The palette transitions from a deep, authoritative navy sidebar into a light, airy canvas. We use color not just for decoration, but to define the physical environment of the app.

### The "No-Line" Rule
To achieve a premium editorial feel, **1px solid borders for sectioning are strictly prohibited.** Do not use lines to separate a sidebar from a header or a card from a background. Instead:
- Define boundaries through background shifts (e.g., a `surface-container-low` section sitting on a `surface` background).
- Use white space as a structural element to create "invisible" gutters.

### Surface Hierarchy & Nesting
Treat the UI as a series of stacked sheets of fine paper. 
- **Base Level:** `surface` (#f9f9f9) serves as the primary floor.
- **Sectioning:** Use `surface-container-low` (#f3f3f3) for large layout blocks.
- **Actionable Elements:** Use `surface-container-lowest` (#ffffff) for cards and primary content containers to make them "pop" against the gray base.
- **Nesting:** If a card contains a sub-section (like a video metadata tag), use `surface-container-high` (#e8e8e8) to create a subtle inset effect.

### The "Glass & Gradient" Rule
Standard flat buttons are for utilities; Grace is a platform for creators. 
- **Signature CTAs:** Use a subtle linear gradient for primary actions, transitioning from `primary` (#0057c2) to `primary-container` (#006ef2). This adds "soul" and a sense of pressable depth.
- **Floating Overlays:** Use `surface-container-lowest` with an 80% opacity and a `backdrop-blur` of 12px for modal headers or floating navigation to keep the video content visible underneath.

---

## 3. Typography: Editorial Authority
We pair **Manrope** (Headings) with **Inter** (Body) to create a high-contrast, professional hierarchy.

- **Display & Headlines (Manrope):** Use `display-md` (2.75rem) for main dashboard greetings and `headline-sm` (1.5rem) for video titles. The geometric nature of Manrope adds a modern, architectural feel.
- **Titles & Labels (Inter):** Use `title-sm` (1rem) for card headers and `label-md` (0.75rem) for metadata. 
- **Body Text:** Consistent `body-md` (0.875rem / 14px) ensures readability for recipe descriptions and distribution notes. 
- **Intentional Contrast:** Always pair a bold `headline-sm` with a light `body-sm` to create clear visual entry points for the eye.

---

## 4. Elevation & Depth: Tonal Layering
We move away from the "shadow-on-everything" approach. Depth is a narrative tool.

- **The Layering Principle:** Avoid traditional shadows for static layout elements. Place a `surface-container-lowest` card on a `surface-container-low` background to create a "soft lift."
- **Ambient Shadows:** Only use shadows for interactive "floating" elements (e.g., dropdowns, modals). Use a highly diffused shadow: `0 8px 32px rgba(0, 26, 67, 0.06)`. The tint should be a whisper of the `on-surface` color, never pure black.
- **The "Ghost Border" Fallback:** If a divider is required for accessibility (e.g., in a high-density table), use the `outline-variant` token at 15% opacity. It should be felt, not seen.
- **Glassmorphism:** For the left sidebar (Dark Navy #001529), use a slight transparency if video content is playing behind it, creating a "frosted glass" depth that feels high-end.

---

## 5. Components & Interaction
Components must feel tactile and responsive.

- **Buttons:** 
    - **Primary:** Gradient fill (`primary` to `primary-container`), 6px radius (`sm`), white text. 
    - **Secondary:** `surface-container-high` background with `on-surface` text. No border.
- **Video Cards:** 
    - Use `8px` (`DEFAULT`) border radius. 
    - **No Dividers:** Separate the thumbnail from the text using a `1.4rem` (`4`) spacing gap.
    - **Hover State:** On hover, transition the background from `surface-container-lowest` to `surface-bright` and apply an ambient shadow.
- **Input Fields:** 
    - Use `surface-container-low` for the field background with a `6px` radius. 
    - On focus, transition to a "Ghost Border" of `primary` at 40% opacity.
- **Chips (Status):** 
    - Use `tertiary-fixed` (#ecdcff) for "Promoted" videos and `secondary-fixed` (#d1e4ff) for "Drafts." These soft, filled backgrounds are more sophisticated than harsh outlines.
- **Video Progress Bars:** 
    - Use a `primary` (#0057c2) fill on a `surface-container-highest` track. For a premium touch, add a subtle glow to the progress indicator.

---

## 6. Do’s and Don’ts

### Do:
- **Use Asymmetric Spacing:** Use larger spacing (`8.5rem` / `24`) at the top of pages and smaller spacing between related cards to create an editorial rhythm.
- **Embrace Whitespace:** If you think there is enough space, add 20% more. Food bloggers value "breathable" layouts.
- **Align to Typography:** Ensure icons and buttons align perfectly to the x-height of your Inter body text.

### Don’t:
- **Don’t use 1px solid black/gray borders:** It breaks the "Curator" aesthetic and makes the app look like a legacy tool.
- **Don’t use standard "Drop Shadows":** Avoid the default `0 2px 4px`. Use our Tonal Layering or Ambient Shadow rules instead.
- **Don’t crowd the Sidebar:** The dark navy sidebar is a sanctuary of focus. Use `5.5rem` (`16`) vertical spacing between major navigation groups.