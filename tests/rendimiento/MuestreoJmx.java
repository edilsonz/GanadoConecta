import com.sun.tools.attach.VirtualMachine;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Set;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

class MuestreoJmx {
    public static void main(String[] args) throws Exception {
        var vm = VirtualMachine.attach(args[0]);
        String url;
        try {
            url = vm.startLocalManagementAgent();
        } finally {
            vm.detach();
        }
        try (var cliente = JMXConnectorFactory.connect(new JMXServiceURL(url));
             var archivo = Files.newBufferedWriter(Path.of(args[1]))) {
            var servidor = cliente.getMBeanServerConnection();
            var tomcat = buscar(servidor, "Tomcat:type=ThreadPool,*");
            var solicitudes = buscar(servidor, "Tomcat:type=GlobalRequestProcessor,*");
            var hikari = buscar(servidor, "com.zaxxer.hikari:type=Pool (*)");
            var heap = new ObjectName("java.lang:type=Memory");
            var hilos = new ObjectName("java.lang:type=Threading");
            var gc = servidor.queryNames(new ObjectName("java.lang:type=GarbageCollector,*"), null);
            archivo.write("utc,hilos_ocupados,hilos_totales,hilos_limite,tcp_tomcat,tcp_limite,cola_configurada,"
                + "peticiones_servidor,errores_servidor,procesamiento_ms,maximo_ms,hikari_activas,hikari_total,"
                + "hikari_esperando,heap_usado_bytes,gc_ms,gc_ciclos,java_hilos");
            archivo.newLine();
            while (true) {
                var memoria = (CompositeData) servidor.getAttribute(heap, "HeapMemoryUsage");
                archivo.write(String.join(",", Instant.now().toString(),
                    valores(servidor, tomcat, "currentThreadsBusy", "currentThreadCount", "maxThreads",
                        "connectionCount", "maxConnections", "acceptCount"),
                    valores(servidor, solicitudes, "requestCount", "errorCount", "processingTime", "maxTime"),
                    valores(servidor, hikari, "ActiveConnections", "TotalConnections", "ThreadsAwaitingConnection"),
                    memoria.get("used").toString(), Long.toString(suma(servidor, gc, "CollectionTime")),
                    Long.toString(suma(servidor, gc, "CollectionCount")), valores(servidor, hilos, "ThreadCount")));
                archivo.newLine();
                archivo.flush();
                Thread.sleep(1000);
            }
        }
    }

    private static ObjectName buscar(MBeanServerConnection servidor, String patron) throws Exception {
        var candidatos = servidor.queryNames(new ObjectName(patron), null);
        if (candidatos.size() != 1) throw new IllegalStateException("MBean ausente o ambiguo: " + patron);
        return candidatos.iterator().next();
    }

    private static String valores(MBeanServerConnection servidor, ObjectName mbean, String... campos) throws Exception {
        var salida = new String[campos.length];
        for (int i = 0; i < campos.length; i++) {
            try {
                salida[i] = servidor.getAttribute(mbean, campos[i]).toString();
            } catch (javax.management.AttributeNotFoundException error) {
                salida[i] = "";
            }
        }
        return String.join(",", salida);
    }

    private static long suma(MBeanServerConnection servidor, Set<ObjectName> beans, String campo) throws Exception {
        long total = 0;
        for (var bean : beans) total += (Long) servidor.getAttribute(bean, campo);
        return total;
    }
}
