import os
from PIL import Image, ImageDraw, ImageFont, ImageFilter

def create_rounded_mask(size, radius):
    mask = Image.new("L", size, 0)
    draw = ImageDraw.Draw(mask)
    draw.rounded_rectangle([(0, 0), size], radius=radius, fill=255)
    return mask

def create_clean_phone_mockup(screenshot_path, target_width, target_height, corner_radius=28, bezel_width=3):
    """
    Creates a realistic modern flagship phone mockup with an accurately 
    placed and rendered camera punch-hole lens.
    """
    ss = Image.open(screenshot_path).convert("RGBA")
    ss = ss.resize((target_width, target_height), Image.Resampling.LANCZOS)
    
    # Clip screenshot with smooth rounded corners
    screen_mask = create_rounded_mask((target_width, target_height), corner_radius - 2)
    
    total_w = target_width + bezel_width * 2
    total_h = target_height + bezel_width * 2
    
    device = Image.new("RGBA", (total_w, total_h), (0, 0, 0, 0))
    d_draw = ImageDraw.Draw(device)
    
    # Outer refined phone chassis (Matte Charcoal / Dark Titanium)
    d_draw.rounded_rectangle(
        [(0, 0), (total_w - 1, total_h - 1)],
        radius=corner_radius,
        fill=(18, 16, 15, 255),
        outline=(52, 48, 44, 255),
        width=1
    )
    
    # Paste screenshot inside bezel
    device.paste(ss, (bezel_width, bezel_width), screen_mask)
    
    # Subtle 1px inner screen highlight
    d_draw.rounded_rectangle(
        [(bezel_width, bezel_width), (bezel_width + target_width - 1, bezel_width + target_height - 1)],
        radius=corner_radius - 2,
        outline=(255, 255, 255, 18),
        width=1
    )
    
    # ── Realistic Front Camera Punch-hole ──
    # Vertically centered in the status bar (approx 2.3% from top)
    cam_diameter = int(target_height * 0.019)
    cam_r = cam_diameter // 2
    cam_cx = total_w // 2
    cam_cy = bezel_width + int(target_height * 0.024)
    
    # 1. Dark outer camera ring
    d_draw.ellipse(
        [(cam_cx - cam_r - 1, cam_cy - cam_r - 1), (cam_cx + cam_r + 1, cam_cy + cam_r + 1)],
        fill=(4, 4, 6, 255)
    )
    # 2. Camera lens glass
    d_draw.ellipse(
        [(cam_cx - cam_r, cam_cy - cam_r), (cam_cx + cam_r, cam_cy + cam_r)],
        fill=(10, 12, 16, 255),
        outline=(25, 30, 40, 200),
        width=1
    )
    # 3. Optical reflection dot (specular glint)
    glint_size = max(1, cam_r // 3)
    d_draw.ellipse(
        [(cam_cx - cam_r + 2, cam_cy - cam_r + 2), (cam_cx - cam_r + 2 + glint_size, cam_cy - cam_r + 2 + glint_size)],
        fill=(120, 160, 220, 180)
    )
    
    return device

def apply_drop_shadow(image, offset=(0, 20), blur=30, shadow_color=(0, 0, 0, 220)):
    w, h = image.size
    pad = blur * 2 + max(abs(offset[0]), abs(offset[1])) + 20
    canvas_w = w + pad * 2
    canvas_h = h + pad * 2
    
    shadow_layer = Image.new("RGBA", (canvas_w, canvas_h), (0, 0, 0, 0))
    alpha = image.split()[3]
    
    s_mask = Image.new("RGBA", image.size, shadow_color)
    s_mask.putalpha(alpha)
    
    pos_x = pad + offset[0]
    pos_y = pad + offset[1]
    shadow_layer.paste(s_mask, (pos_x, pos_y))
    shadow_layer = shadow_layer.filter(ImageFilter.GaussianBlur(blur))
    
    # Paste actual image
    shadow_layer.paste(image, (pad, pad), image)
    return shadow_layer, pad

def main():
    SCALE = 2
    W, H = 1024 * SCALE, 500 * SCALE
    
    # Solid, clean flat dark background
    BG_COLOR = (9, 9, 11, 255) # #09090B
    canvas = Image.new("RGBA", (W, H), BG_COLOR)
    draw = ImageDraw.Draw(canvas)
    
    # 1. Typography Setup
    font_dir = "app/app/src/main/res/font"
    space_grotesk = os.path.join(font_dir, "space_grotesk_variable.ttf")
    ibm_plex_sans = os.path.join(font_dir, "ibm_plex_sans_variable.ttf")
    
    try:
        title_font = ImageFont.truetype(space_grotesk, 56 * SCALE)
        subtitle_font = ImageFont.truetype(ibm_plex_sans, 22 * SCALE)
    except Exception:
        title_font = subtitle_font = ImageFont.load_default()
    
    # 2. Brand Emblem (Red icon on dark container)
    icon_path = "assets/play_store_icon_512.png"
    if os.path.exists(icon_path):
        icon_size = 64 * SCALE
        icon = Image.open(icon_path).convert("RGBA")
        icon = icon.resize((icon_size, icon_size), Image.Resampling.LANCZOS)
        icon_mask = create_rounded_mask((icon_size, icon_size), 18 * SCALE)
        
        ix, iy = 70 * SCALE, 80 * SCALE
        canvas.paste(icon, (ix, iy), icon_mask)
        draw.rounded_rectangle(
            [(ix, iy), (ix + icon_size, iy + icon_size)],
            radius=18 * SCALE,
            outline=(45, 42, 38, 255),
            width=2 * SCALE
        )
    
    # 3. Editorial Typography
    tx = 70 * SCALE
    ty = 180 * SCALE
    
    # Title
    draw.text(
        (tx, ty),
        "Music Without\nCompromise",
        font=title_font,
        fill=(245, 240, 236, 255),
        spacing=10 * SCALE,
        stroke_width=int(1.2 * SCALE),
        stroke_fill=(245, 240, 236, 255)
    )
    
    # Subtitle
    draw.text(
        (tx, ty + int(140 * SCALE)),
        "Own your library. Tune every detail.",
        font=subtitle_font,
        fill=(168, 162, 154, 255)
    )
    
    # 4. Device Mockups on Right
    now_playing_ss = "assets/screenshots/06_now_playing.png"
    library_ss = "assets/screenshots/02_library_albums.png"
    
    # Back Phone (Library)
    if os.path.exists(library_ss):
        p2_w = int(204 * SCALE)
        p2_h = int(453 * SCALE)
        phone2 = create_clean_phone_mockup(
            library_ss, 
            p2_w, 
            p2_h, 
            corner_radius=int(22 * SCALE), 
            bezel_width=int(3 * SCALE)
        )
        shadow2, pad2 = apply_drop_shadow(phone2, offset=(0, int(16 * SCALE)), blur=int(24 * SCALE), shadow_color=(0, 0, 0, 210))
        
        p2_x = int(755 * SCALE) - pad2
        p2_y = int(50 * SCALE) - pad2
        canvas.paste(shadow2, (p2_x, p2_y), shadow2)
        
    # Front Phone (Now Playing - Hero device)
    if os.path.exists(now_playing_ss):
        p1_w = int(224 * SCALE)
        p1_h = int(498 * SCALE)
        phone1 = create_clean_phone_mockup(
            now_playing_ss, 
            p1_w, 
            p1_h, 
            corner_radius=int(24 * SCALE), 
            bezel_width=int(3.5 * SCALE)
        )
        shadow1, pad1 = apply_drop_shadow(phone1, offset=(int(-8 * SCALE), int(24 * SCALE)), blur=int(28 * SCALE), shadow_color=(0, 0, 0, 240))
        
        p1_x = int(525 * SCALE) - pad1
        p1_y = int(15 * SCALE) - pad1
        canvas.paste(shadow1, (p1_x, p1_y), shadow1)
    
    # Final downsample with Lanczos for subpixel antialiasing
    final_graphic = canvas.resize((1024, 500), Image.Resampling.LANCZOS).convert("RGB")
    
    output_path = "assets/feature_graphic_1024x500.png"
    final_graphic.save(output_path, "PNG", quality=95)
    print(f"Generated realistic punch-hole graphic: {output_path}")

if __name__ == "__main__":
    main()
