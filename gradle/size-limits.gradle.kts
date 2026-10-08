// Rào chắn chống file/hàm phình lại (Giai đoạn 8 – docs/REFACTOR_SPLIT_FILES_PLAN.md).
//
//   ./gradlew checkSizeLimits     (cũng chạy trong ./gradlew :app:check)
//
// Báo lỗi khi trong app/src/main có file .kt dài quá MAX_FILE_LINES dòng hoặc hàm (kể cả @Composable,
// hàm dựng LazyListScope...) dài quá MAX_FUNCTION_LINES dòng. Các vi phạm có từ trước được ghi trong
// config/size-limits-baseline.txt kèm số dòng hiện tại: chỉ được GIẢM, không được tăng; vi phạm mới → lỗi.
// Không cần plugin/thư viện ngoài, chạy được với configuration cache.

val MAX_FILE_LINES = 600
val MAX_FUNCTION_LINES = 250

@DisableCachingByDefault(because = "Chỉ đọc mã nguồn và in báo cáo, không tạo file")
abstract class CheckSizeLimitsTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sources: ConfigurableFileCollection

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val baselineFile: RegularFileProperty

    /** Thư mục gốc dự án – để in và so khớp đường dẫn tương đối trong baseline. */
    @get:Internal
    abstract val projectRoot: DirectoryProperty

    @get:Input
    abstract val maxFileLines: Property<Int>

    @get:Input
    abstract val maxFunctionLines: Property<Int>

    @TaskAction
    fun check() {
        val root = projectRoot.get().asFile
        val baseline = readBaseline(baselineFile.get().asFile)
        val seen = mutableSetOf<String>()
        val errors = mutableListOf<String>()
        val notes = mutableListOf<String>()

        fun judge(key: String, label: String, lines: Int, limit: Int) {
            val allowed = baseline[key]
            if (allowed != null) seen += key
            when {
                lines <= limit ->
                    if (allowed != null) notes += "$label: còn $lines dòng (≤ $limit) → xóa dòng \"$key\" khỏi baseline"
                allowed == null ->
                    errors += "$label: $lines dòng (giới hạn $limit)"
                lines > allowed ->
                    errors += "$label: $lines dòng – đã tăng so với baseline ($allowed). Hãy tách nhỏ thay vì sửa baseline."
                lines < allowed ->
                    notes += "$label: giảm từ $allowed xuống $lines dòng → hạ số trong baseline xuống $lines"
            }
        }

        sources.files.filter { it.extension == "kt" }.sortedBy { it.path }.forEach { file ->
            val path = file.relativeTo(root).invariantSeparatorsPath
            val text = file.readText()
            judge("file $path", path, countLines(text), maxFileLines.get())
            // Hàm trùng tên trong cùng file (overload) tính theo hàm dài nhất
            longestFunctions(text).forEach { (name, info) ->
                val (line, length) = info
                judge("fun $path#$name", "$path:$line $name()", length, maxFunctionLines.get())
            }
        }
        (baseline.keys - seen).forEach { notes += "Baseline có \"$it\" nhưng không còn tìm thấy → xóa dòng này" }

        notes.forEach { logger.lifecycle("ℹ️  $it") }
        if (errors.isNotEmpty()) {
            throw GradleException(
                "Vượt giới hạn kích thước code (file ${maxFileLines.get()} dòng, hàm ${maxFunctionLines.get()} dòng):\n" +
                    errors.joinToString("\n") { "  ✗ $it" } +
                    "\nXem hướng dẫn tách file trong CONTRIBUTING.md."
            )
        }
        logger.lifecycle("OK: không có file > ${maxFileLines.get()} dòng hay hàm > ${maxFunctionLines.get()} dòng mới (baseline: ${baseline.size} ngoại lệ).")
    }

    /** Mỗi dòng: "file <đường dẫn> <số dòng tối đa>" hoặc "fun <đường dẫn>#<tên hàm> <số dòng tối đa>"; "#" ở đầu dòng là chú thích. */
    private fun readBaseline(file: File): Map<String, Int> =
        file.readLines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .associate { line ->
                val key = line.substringBeforeLast(' ').trim()
                val max = line.substringAfterLast(' ').toIntOrNull()
                    ?: throw GradleException("Dòng baseline không hợp lệ: \"$line\"")
                key to max
            }

    /** Giống `wc -l`, nhưng tính cả dòng cuối không có ký tự xuống dòng. */
    private fun countLines(text: String): Int =
        text.count { it == '\n' } + if (text.isNotEmpty() && !text.endsWith('\n')) 1 else 0

    /** Tên hàm → (dòng bắt đầu, số dòng) của hàm dài nhất mang tên đó. Chỉ xét hàm có thân `{ ... }`. */
    private fun longestFunctions(text: String): Map<String, Pair<Int, Int>> {
        val code = blankStringsAndComments(text)
        val result = mutableMapOf<String, Pair<Int, Int>>()
        val header = Regex("""\bfun\s+(?:<[^>]*>\s*)?(?:[\w.]+\.)?(\w+)\s*\(""")
        val afterParams = Regex("""\s*(?::\s*[^={\n]+)?\s*([{=])""")
        for (m in header.findAll(code)) {
            val close = matching(code, m.range.last, '(', ')') ?: continue
            val tail = afterParams.find(code, close + 1)?.takeIf { it.range.first == close + 1 } ?: continue
            if (tail.groupValues[1] != "{") continue // hàm dạng biểu thức "= ..."
            val open = tail.groups[1]!!.range.first
            val end = matching(code, open, '{', '}') ?: continue
            val startLine = lineOf(code, m.range.first)
            val length = lineOf(code, end) - startLine + 1
            val name = m.groupValues[1]
            if (length > (result[name]?.second ?: 0)) result[name] = startLine to length
        }
        return result
    }

    private fun matching(code: String, openIndex: Int, open: Char, close: Char): Int? {
        var depth = 0
        for (i in openIndex until code.length) {
            when (code[i]) {
                open -> depth++
                close -> if (--depth == 0) return i
            }
        }
        return null
    }

    private fun lineOf(code: String, index: Int): Int = code.substring(0, index).count { it == '\n' } + 1

    /** Thay nội dung chuỗi, ký tự và chú thích bằng khoảng trắng (giữ xuống dòng) để đếm ngoặc không bị nhầm. Mã trong `${...}` được giữ. */
    private fun blankStringsAndComments(src: String): String {
        val out = StringBuilder(src.length)
        val modes = ArrayDeque<Char>()          // 'S' chuỗi "...", 'R' chuỗi """...""", 'T' mã trong ${...}
        val templateBraces = ArrayDeque<Int>()  // số '{' đang mở bên trong từng ${...}
        fun blank(from: Int, to: Int) {
            for (k in from until minOf(to, src.length)) out.append(if (src[k] == '\n') '\n' else ' ')
        }
        var i = 0
        while (i < src.length) {
            val c = src[i]
            val next = src.getOrElse(i + 1) { ' ' }
            val mode = modes.lastOrNull()
            if (mode == null || mode == 'T') {
                when {
                    c == '/' && next == '/' -> {
                        val end = src.indexOf('\n', i).let { if (it < 0) src.length else it }
                        blank(i, end); i = end
                    }
                    c == '/' && next == '*' -> {
                        val end = src.indexOf("*/", i + 2).let { if (it < 0) src.length else it + 2 }
                        blank(i, end); i = end
                    }
                    src.startsWith("\"\"\"", i) -> { modes.addLast('R'); blank(i, i + 3); i += 3 }
                    c == '"' -> { modes.addLast('S'); blank(i, i + 1); i++ }
                    c == '\'' -> {
                        // Ký tự: 'a', '\n', 'A', '{', '"'
                        val end = if (next == '\\') src.indexOf('\'', i + 3) else if (src.getOrNull(i + 2) == '\'') i + 2 else -1
                        if (end > 0) { blank(i, end + 1); i = end + 1 } else { out.append(c); i++ }
                    }
                    mode == 'T' && c == '{' -> { templateBraces.addLast(templateBraces.removeLast() + 1); out.append(c); i++ }
                    mode == 'T' && c == '}' -> {
                        val depth = templateBraces.removeLast()
                        if (depth == 0) { modes.removeLast(); blank(i, i + 1) } else { templateBraces.addLast(depth - 1); out.append(c) }
                        i++
                    }
                    else -> { out.append(c); i++ }
                }
            } else {
                when {
                    mode == 'S' && c == '\\' -> { blank(i, i + 2); i += 2 }
                    c == '$' && next == '{' -> { modes.addLast('T'); templateBraces.addLast(0); blank(i, i + 2); i += 2 }
                    mode == 'S' && c == '"' -> { modes.removeLast(); blank(i, i + 1); i++ }
                    mode == 'R' && src.startsWith("\"\"\"", i) -> {
                        var k = i + 3
                        while (k < src.length && src[k] == '"') k++ // """" ở cuối: các dấu " thừa thuộc về chuỗi
                        blank(i, k); i = k; modes.removeLast()
                    }
                    else -> { blank(i, i + 1); i++ }
                }
            }
        }
        return out.toString()
    }
}

tasks.register<CheckSizeLimitsTask>("checkSizeLimits") {
    group = "verification"
    description = "Báo file .kt > $MAX_FILE_LINES dòng và hàm > $MAX_FUNCTION_LINES dòng mới trong app/src/main (xem config/size-limits-baseline.txt)."
    sources.from(fileTree("app/src/main") { include("**/*.kt") })
    baselineFile.set(layout.projectDirectory.file("config/size-limits-baseline.txt"))
    projectRoot.set(layout.projectDirectory)
    maxFileLines.set(MAX_FILE_LINES)
    maxFunctionLines.set(MAX_FUNCTION_LINES)
}
