from pathlib import Path

def python_to_java_hex_byte_array(
        input_file,
        output_file="Output.java",
        variable_name="PYTHON_BYTES"
):

    # Read file as raw bytes
    data = Path(input_file).read_bytes()

    hex_values = []

    for byte in data:
        hex_values.append(f"(byte)0x{byte:02X}")

    # Split into formatted lines
    lines = []
    chunk_size = 16

    for i in range(0, len(hex_values), chunk_size):

        chunk = ", ".join(
            hex_values[i:i + chunk_size]
        )

        lines.append("        " + chunk)

    joined_lines = ",\n".join(lines)

    java_code = f"""public class PythonBytes {{

    public static final byte[] {variable_name} = {{
{joined_lines}
    }};

}}
"""

    Path(output_file).write_text(
        java_code,
        encoding="utf-8"
    )

    print(f"[+]: Generated -> {output_file}")
    print(f"[+]: Total Bytes -> {len(data)}")


if __name__ == "__main__":
    python_to_java_hex_byte_array(
        "app.py",
        "MediaPipeServer.java",
        "SERVER_SCRIPT"
    )