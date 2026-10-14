import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Motor analitico de procesamiento por lotes y reporteria transaccional.
 * Realiza la ingesta de catalogos maestros, consolida las ventas individuales
 * y exporta los reportes finales ordenados descendentemente.
 * 
 * Satisface los requerimientos de la segunda clase con metodo main para la
 * Entrega Final del modulo Conceptos Fundamentales de Programacion.
 * 
 * @author Jaime Alberto Rodriguez Jaramillo
 * @version 3.0
 */
public class Main {

    /**
     * Modela un producto del catalogo comercial y acumula las unidades vendidas.
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

        public String getId() { return id; }
        public String getNombre() { return nombre; }
        public double getPrecio() { return precio; }
        public int getCantidadVendida() { return cantidadVendida; }

        public void sumarCantidad(int cantidad) {
            this.cantidadVendida += cantidad;
        }
    }

    /**
     * Modela a un vendedor y totaliza el monto monetario recaudado por sus ventas.
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

        public String getTipoDoc() { return tipoDoc; }
        public String getNumDoc() { return numDoc; }
        public String getNombres() { return nombres; }
        public String getApellidos() { return apellidos; }
        public double getVentasTotales() { return ventasTotales; }

        public void sumarVenta(double monto) {
            this.ventasTotales += monto;
        }
    }

    /**
     * Punto de entrada del motor analitico.
     * 
     * @param args Argumentos de linea de comandos (no utilizados).
     */
    public static void main(String[] args) {
        try {
            System.out.println("Iniciando procesamiento analitico de ventas...");

            // 1. Carga de catalogos maestros indexados en memoria O(1)
            Map<String, Producto> mapaProductos = cargarProductos("productos.csv");
            Map<String, Vendedor> mapaVendedores = cargarVendedores("vendedores.csv");

            // 2. Localizacion y procesamiento de archivos de ventas individuales
            procesarArchivosDeVentas(mapaVendedores, mapaProductos);

            // 3. Generacion de los reportes CSV solicitados
            generarReportes(mapaVendedores, mapaProductos);

            System.out.println("Procesamiento finalizado con exito. Reportes generados correctamente!");
        } catch (Exception e) {
            System.err.println("ERROR durante el procesamiento de datos: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Carga el catalogo maestro de productos desde el archivo CSV.
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
     * Carga el archivo maestro de vendedores desde el archivo CSV.
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
     * Localiza todos los archivos individuales de venta cuyo nombre coincida con
     * el patron 'vendedor_*.csv' en el directorio de trabajo.
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
     * Procesa las transacciones de un archivo de vendedor particular.
     * Incorpora el Punto Extra (c): control y descarte seguro de datos anomalos.
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
                if (campos.length < 2) continue; // Omite lineas truncadas

                try {
                    String idProducto = campos[0].trim();
                    int cantidad = Integer.parseInt(campos[1].trim());

                    // Validacion Punto Extra (c): Cantidades incoherentes o negativas
                    if (cantidad <= 0) {
                        System.err.println("Advertencia [" + archivo.getFileName() + "]: Cantidad invalida omitida (" + cantidad + ")");
                        continue;
                    }

                    // Validacion Punto Extra (c): Integridad referencial de productos
                    Producto producto = mapaProductos.get(idProducto);
                    if (producto == null) {
                        System.err.println("Advertencia [" + archivo.getFileName() + "]: Producto con ID " + idProducto + " no existe. Registro omitido.");
                        continue;
                    }

                    // Calculo y acumulacion segura en memoria
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
     * Genera los dos reportes finales ordenados descendentemente (Requisitos 3 y 4).
     */
    private static void generarReportes(Map<String, Vendedor> mapaVendedores, Map<String, Producto> mapaProductos) throws IOException {
        // 1. reporte_vendedores.csv ordenado de mayor a menor recaudacion
        List<Vendedor> vendedoresOrdenados = mapaVendedores.values().stream()
                .sorted(Comparator.comparingDouble(Vendedor::getVentasTotales).reversed())
                .collect(Collectors.toList());

        try (PrintWriter writer = new PrintWriter("reporte_vendedores.csv", StandardCharsets.UTF_8.name())) {
            for (Vendedor v : vendedoresOrdenados) {
                writer.printf(Locale.US, "%s %s;%.2f\n", v.getNombres(), v.getApellidos(), v.getVentasTotales());
            }
        }

        // 2. reporte_productos.csv ordenado por cantidad vendida de mayor a menor
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