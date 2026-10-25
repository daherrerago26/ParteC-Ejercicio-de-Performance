// Evalúa los criterios del enunciado sobre el statistics.json que genera el dashboard de JMeter (fila Total).
// Uso: java -cp "<JMETER_HOME>/lib/*" groovy.ui.GroovyMain scripts/evaluate-criteria.groovy <statistics.json> <criterios.txt>
// Termina con código 0 si se cumplen todos los criterios y con 1 si alguno falla.
// Las tildes de los textos del reporte se escriben como secuencias de escape Unicode, para que no dependan
// de la codificación con la que Java lea este archivo.

import groovy.json.JsonSlurper

import java.nio.charset.StandardCharsets

if (args.length != 2) {
    System.err.println('Usage: evaluate-criteria.groovy <statistics.json> <output.txt>')
    System.exit(2)
}

def total = new JsonSlurper().parse(new File(args[0]))['Total']

def criteria = [
        [name: 'Al menos 20 TPS', value: String.format('%.2f TPS', total.throughput), condition: '>= 20 TPS',
         passed: total.throughput >= 20],
        [name: 'Tasa de error menor al 3 %', value: String.format('%.2f %%', total.errorPct), condition: '< 3 %',
         passed: total.errorPct < 3],
        [name: 'Tiempo de respuesta m\u00e1ximo de 1,5 s', value: String.format('%.0f ms', total.maxResTime),
         condition: '<= 1500 ms', passed: total.maxResTime <= 1500],
]
def allPassed = criteria.every { it.passed }

def report = new StringBuilder()
report << 'CRITERIOS DEL ENUNCIADO - PRUEBA DE CARGA DEL LOGIN\n'
report << '===================================================\n'
report << "Fuente: ${args[0]} (fila Total)\n\n"
report << String.format('%-38s %-12s %-12s %s\n', 'Criterio', 'Valor', 'Condici\u00f3n', 'Resultado')
criteria.each { c ->
    report << String.format('%-38s %-12s %-12s %s\n', c.name, c.value, c.condition, c.passed ? 'CUMPLE' : 'NO CUMPLE')
}
report << '\nDatos de apoyo\n'
report << String.format('  Peticiones: %d, errores: %d\n', total.sampleCount as long, total.errorCount as long)
report << String.format('  Tiempo de respuesta (ms): m\u00ednimo %.0f, promedio %.0f, mediana %.0f, p90 %.0f, p95 %.0f, p99 %.0f, m\u00e1ximo %.0f\n',
        total.minResTime, total.meanResTime, total.medianResTime, total.pct1ResTime, total.pct2ResTime,
        total.pct3ResTime, total.maxResTime)
report << "\nRESULTADO GENERAL: ${allPassed ? 'CUMPLE todos los criterios' : 'NO CUMPLE alguno de los criterios'}\n"

new File(args[1]).setText(report.toString(), StandardCharsets.UTF_8.name())
print report
System.exit(allPassed ? 0 : 1)
