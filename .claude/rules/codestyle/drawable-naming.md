# Image Resources

## Naming

There are 3 types of icons:

1. Black or single color icon (naming: `ic_name_24`, where number is size)
2. Icon with constant color, and tint could be applied (naming: `img_name_24`)
3. Large image with different colors and shapes (naming: `ill_name`)

Examples:

1. `ic_chevron_24`
2. `img_walletconnect_24`
3. `ill_bussiness`

## Attention

For complex vector images (named with `ill_name`), you should use `.png` resources, because when the project is compiled, all complex vectors are converted to large, heavy PNGs for different dimensions.