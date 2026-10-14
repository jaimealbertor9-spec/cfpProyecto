import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Motor analitico de procesamiento por lotes y consolidacion de reporteria comercial.
 * Lee los catalogos maestros, procesa los archivos de transacciones por vendedor
 * y genera los reportes analiticos ordenados descendentemente.
 * 
 * Satisface los requisitos de la segunda clase con metodo main para la
 * Entrega Final del modulo Conceptos Fundamentales de Programacion.
 * 
 * @author Jaime Alberto Rodriguez Jaramillo
 * @version 3.0
 */
public class Main {

    /**
     * Clase de dominio que encapsula las propiedades de un Producto
     * y totaliza las unidades vendidas en memoria.
     */
    public static class Producto {
        private final String id;
        private final String nombre;
        private final double precio;
        private int cantidadVendida;

        public Producto(String id, String nombre, double precio) {
            this.id = id;
            this.nombre = nombre;
            this.precio = precio;
            this.cantidadVendida = 0;
        }

        public String getId() {
            return id;
        }

        public String getNombre() {
            return nombre;
        }

        public double getPrecio() {
            return precio;
        }

        public int getCantidadVendida() {
            return cantidadVendida;
        }

        public void sumarCantidad(int cantidad) {
            this.cantidadVendida += cantidad;
        }
    }

    /**
     * Clase de dominio que representa a un Vendedor y totaliza el monto
     * monetario recaudado por concepto de sus ventas.
     */
    public static class Vendedor {
        private final String tipoDoc;
        private final String numDoc;
        private final String nombres;
        private final String apellidos;
        private double ventasTotales;

        public Vendedor(String tipoDoc, String numDoc, String nombres, String apellidos) {
            this.tipoDoc = tipoDoc;
            this.numDoc = numDoc;
            this.nombres = nombres;
            this.apellidos = apellidos;
            this.ventasTotales = 0.0;
        }

        public String getTipoDoc() {
            return tipoDoc;
        }

        public String getNumDoc() {
            return numDoc;
        }

        public String getNombres() {
            return nombres;
        }

        public String getApellidos() {
            return apellidos;
        }

        public double getVentasTotales() {
            return ventasTotales;
        }

        public void sumarVenta(double monto) {
            this.ventasTotales += monto;
        }
    }

    /**
     * Metodo principal de ejecucion del motor analitico.
     * Procesa los datos y genera los archivos de salida sin solicitar datos al usuario.
     * 
     * @param args Argumentos de linea de comandos (no utilizados).
     */
    public static void main(String[] args) {
        try {
            System.out.println("Iniciando procesamiento analitico de ventas...");

            // 1. Carga de catalogos maestros en memoria mediante tablas Hash O(1)
            Map<String, Producto> mapaProductos = cargarProductos("productos.csv");
            Map<String, Vendedor> mapaVendedores = cargarVendedores("vendedores.csv");

            // 2. Busqueda y procesamiento de los archivos individuales de venta
            procesarArchivosDeVentas(mapaVendedores, mapaProductos);

            // 3. Generacion de los reportes CSV consolidados
            generarReportes(mapaVendedores, mapaProductos);

            System.out.println("Procesamiento finalizado con exito. Reportes generados correctamente!");
        } catch (Exception e) {
            System.err.println("ERROR durante el procesamiento de datos: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Carga el archivo maestro de productos hacia un Map indexado por ID.
     */
    private static Map<String, Producto> cargarProductos(String ruta) throws IOException {
        Map<String, Producto> mapa = new HashMap<>();
        Path path = Paths.get(ruta);
        if (!Files.exists(path)) {
            throw new IOException("Archivo maestro de productos no encontrado: " + ruta);
        }

        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String linea;
            while ((linea = reader.readLine()) != null) {
                if (linea.trim().isEmpty()) continue;
                String[] partes = linea.split(";");
                if (partes.length >= 3) {
                    String id = partes[0].trim();
                    String nombre = partes[1].trim();
                    double precio = Double.parseDouble(partes[2].trim());
                    mapa.put(id, new Producto(id, nombre, precio));
                }
            }
        }
        return mapa;
    }

    /**
     * Carga el archivo maestro de vendedores hacia un Map indexado por Numero de Documento.
     */
    private static Map<String, Vendedor> cargarVendedores(String ruta) throws IOException {
        Map<String, Vendedor> mapa = new HashMap<>();
        Path path = Paths.get(ruta);
        if (!Files.exists(path)) {
            throw new IOException("Archivo maestro de vendedores no encontrado: " + ruta);
        }

        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String linea;
            while ((linea = reader.readLine()) != null) {
                if (linea.trim().isEmpty()) continue;
                String[] partes = linea.split(";");
                if (partes.length >= 4) {
                    String tipoDoc = partes[0].trim();
                    String numDoc = partes[1].trim();
                    String nombres = partes[2].trim();
                    String apellidos = partes[3].trim();
                    mapa.put(numDoc, new Vendedor(tipoDoc, numDoc, nombres, apellidos));
                }
            }
        }
        return mapa;
    }

    /**
     * Explora el directorio de trabajo local identificando los archivos con patron 'vendedor_*.csv'.
     */
    private static void procesarArchivosDeVentas(Map<String, Vendedor> mapaVendedores, Map<String, Producto> mapaProductos) throws IOException {
        List<Path> archivos = Files.walk(Paths.get("."))
                .filter(Files::isRegularFile)
                .filter(p -> p.getFileName().toString().matches("^vendedor_.*\\.csv$"))
                .collect(Collectors.toList());

        for (Path archivo : archivos) {
            procesarArchivoVentaIndividual(archivo, mapaVendedores, mapaProductos);
        }
    }

    /**
     * Procesa un archivo particular de ventas aplicando validaciones defensivas (Punto Extra c).
     */
    private static void procesarArchivoVentaIndividual(Path archivo, Map<String, Vendedor> mapaVendedores, Map<String, Producto> mapaProductos) {
        try (BufferedReader reader = Files.newBufferedReader(archivo, StandardCharsets.UTF_8)) {
            String cabecera = reader.readLine();
            if (cabecera == null || cabecera.trim().isEmpty()) return;

            String[] datosCabecera = cabecera.split(";");
            if (datosCabecera.length < 2) return;

            String numDocVendedor = datosCabecera[1].trim();
            Vendedor vendedor = mapaVendedores.get(numDocVendedor);

            if (vendedor == null) {
                System.err.println("Advertencia: El vendedor con documento " + numDocVendedor + " no figura en el catalogo maestro.");
                return;
            }

            String linea;
            while ((linea = reader.readLine()) != null) {
                if (linea.trim().isEmpty()) continue;
                String[] campos = linea.split(";");
                if (campos.length < 2) continue;

                try {
                    String idProducto = campos[0].trim();
                    int cantidad = Integer.parseInt(campos[1].trim());

                    // Validacion: Cantidades menores o iguales a cero
                    if (cantidad <= 0) {
                        System.err.println("Advertencia [" + archivo.getFileName() + "]: Cantidad invalida omitida (" + cantidad + ")");
                        continue;
                    }

                    // Validacion: Integridad referencial de productos
                    Producto producto = mapaProductos.get(idProducto);
                    if (producto == null) {
                        System.err.println("Advertencia [" + archivo.getFileName() + "]: Producto con ID " + idProducto + " no existe. Omitido.");
                        continue;
                    }

                    // Acumulacion de metricas en memoria
                    double subtotal = producto.getPrecio() * cantidad;
                    vendedor.sumarVenta(subtotal);
                    producto.sumarCantidad(cantidad);

                } catch (NumberFormatException e) {
                    System.err.println("Advertencia [" + archivo.getFileName() + "]: Error de formato numerico en linea -> " + linea);
                }
            }
        } catch (IOException e) {
            System.err.println("Error al procesar archivo " + archivo.getFileName() + ": " + e.getMessage());
        }
    }

    /**
     * Genera los dos reportes finales consolidados en formato CSV ordenados descendentemente.
     */
    private static void generarReportes(Map<String, Vendedor> mapaVendedores, Map<String, Producto> mapaProductos) throws IOException {
        // 1. Reporte de vendedores ordenado por total recaudado de mayor a menor
        List<Vendedor> vendedoresOrdenados = mapaVendedores.values().stream()
                .sorted(Comparator.comparingDouble(Vendedor::getVentasTotales).reversed())
                .collect(Collectors.toList());

        try (PrintWriter writer = new PrintWriter("reporte_vendedores.csv", StandardCharsets.UTF_8.name())) {
            for (Vendedor v : vendedoresOrdenados) {
                writer.printf(Locale.US, "%s %s;%.2f\n", v.getNombres(), v.getApellidos(), v.getVentasTotales());
            }
        }

        // 2. Reporte de productos ordenado por cantidad vendida de mayor a menor
        List<Producto> productosOrdenados = mapaProductos.values().stream()
                .sorted(Comparator.comparingInt(Producto::getCantidadVendida).reversed())
                .collect(Collectors.toList());

        try (PrintWriter writer = new PrintWriter("reporte_productos.csv", StandardCharsets.UTF_8.name())) {
            for (Producto p : productosOrdenados) {
                writer.printf(Locale.US, "%s;%.2f\n", p.getNombre(), p.getPrecio());
            }
        }
    }
}