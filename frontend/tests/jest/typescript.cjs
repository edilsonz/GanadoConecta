const ts = require('typescript')

module.exports = {
  process(source, filename) {
    const salida = ts.transpileModule(source, {
      fileName: filename,
      compilerOptions: {
        target: ts.ScriptTarget.ES2022,
        module: ts.ModuleKind.CommonJS,
        sourceMap: true,
        inlineSources: true,
      },
    })
    return { code: salida.outputText, map: salida.sourceMapText }
  },
}
