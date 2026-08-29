#!/bin/bash
set -e
INPUT_DIR="${1:-./source_armors}"
OUTPUT_DIR="${2:-./output_webp}"
QUALITY=82
WIDTH=900

mkdir -p "$OUTPUT_DIR"
count=0

for i in $(seq 1 42); do
  src=""
  for candidate in \
    "$INPUT_DIR/mark_$i.png" \
    "$INPUT_DIR/mark_$i.jpg" \
    "$INPUT_DIR/armor_mark_$i.png" \
    "$INPUT_DIR/armor_mark_$i.jpg" \
    "$INPUT_DIR/Mark$i.png" \
    "$INPUT_DIR/$i.png"
  do
    [ -f "$candidate" ] && src="$candidate" && break
  done

  out="$OUTPUT_DIR/armor_mark_$i.webp"
  if [ -z "$src" ]; then
    echo "SKIP  Mark $i"
    continue
  fi

  cwebp -q "$QUALITY" -m 6 -resize "$WIDTH" 0 "$src" -o "$out"
  echo "OK    armor_mark_$i.webp"
  count=$((count + 1))
done

echo "Converted: $count / 42 → $OUTPUT_DIR/"
