package wcorp.aplicaciones.productos.colocaciones.solicitudes.bo;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Set;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import wcorp.aplicaciones.gestiondenegocios.estrategia.riesgofinanciero.calificaciondeclientes.dao.CalificacionesDAO;
import wcorp.aplicaciones.gestiondenegocios.estrategia.riesgofinanciero.calificaciondeclientes.delegate
    .CalificacionDeClientesDelegate;
import wcorp.aplicaciones.gestiondenegocios.estrategia.riesgofinanciero.calificaciondeclientes.to
    .InfoHistoriaCalificacionTO;
import wcorp.aplicaciones.productos.colocaciones.solicitudes.dao.jdbc.DatosBasicosSolicitudDAO;
import wcorp.aplicaciones.productos.colocaciones.solicitudes.dao.jdbc.EstadoFinancieroYVentasSolicitudDAO;
import wcorp.aplicaciones.productos.colocaciones.solicitudes.dao.jdbc.LineaDeCreditoSolicitudDAO;
import wcorp.aplicaciones.productos.colocaciones.solicitudes.dao.jdbc.ParametrosSolicitudDAO;
import wcorp.aplicaciones.productos.colocaciones.solicitudes.dao.jdbc.SolicitudesDAO;
import wcorp.aplicaciones.productos.colocaciones.solicitudes.delegate.ServiciosSolicitudesDelegate;
import wcorp.aplicaciones.productos.colocaciones.solicitudes.excepciones.SolicitudCreditoException;
import wcorp.aplicaciones.productos.colocaciones.solicitudes.to.AnalisisSolicitudTO;
import wcorp.aplicaciones.productos.colocaciones.solicitudes.to.AtribucionColaboradorTO;
import wcorp.aplicaciones.productos.colocaciones.solicitudes.to.ComportamientoClienteTO;
import wcorp.aplicaciones.productos.colocaciones.solicitudes.to.CuadroDeMandoTO;
import wcorp.aplicaciones.productos.colocaciones.solicitudes.to.EstadoFinancieroTO;
import wcorp.aplicaciones.productos.colocaciones.solicitudes.to.FiltroRiesgoTO;
import wcorp.aplicaciones.productos.colocaciones.solicitudes.to.FiltrosSolicitantePymeTO;
import wcorp.aplicaciones.productos.colocaciones.solicitudes.to.IntegranteGrupoEconomicoTO;
import wcorp.aplicaciones.productos.colocaciones.solicitudes.to.JustificacionSolicitudTO;
import wcorp.aplicaciones.productos.colocaciones.solicitudes.to.LineaDeCreditoGlobalTO;
import wcorp.aplicaciones.productos.colocaciones.solicitudes.to.ObservacionAnalisisSolicitudTO;
import wcorp.aplicaciones.productos.colocaciones.solicitudes.to.OpcionListadoSolicitudTO;
import wcorp.aplicaciones.productos.colocaciones.solicitudes.to.ResolucionSolicitudTO;
import wcorp.aplicaciones.productos.colocaciones.solicitudes.to.RespuestaInvocacionesSolicitudTO;
import wcorp.aplicaciones.productos.colocaciones.solicitudes.to.SolicitantePymeTO;
import wcorp.aplicaciones.productos.colocaciones.solicitudes.to.SolicitudTO;
import wcorp.aplicaciones.productos.colocaciones.solicitudes.to.VentasTO;
import wcorp.aplicaciones.productos.colocaciones.solicitudes.util.SolicitudUtil;
import wcorp.bprocess.workstation.vo.DatosDelClienteVO;
import wcorp.model.actores.cliente.Cliente;
import wcorp.model.actores.cliente.ClientePersona;
import wcorp.serv.clientes.DatosEmpresa;
import wcorp.serv.riesgo.RiesgoDelegate;
import wcorp.util.ErroresUtil;
import wcorp.util.FechasUtil;
import wcorp.util.GeneralException;
import wcorp.util.RUTUtil;
import wcorp.util.StringUtil;
import wcorp.util.TablaValores;

/**
 * Objeto de negocio encargado de controlar la lógica de alertas de una solicitud
 * <p>
 * Registro de versiones:
 * <ul>
 * <li>1.0 26/05/2014 Gonzalo Bustamante, Mónica Garcés.(Sermaluc): Versión inicial.</li>
 * <li>1.1 12/09/2014 Gonzalo Bustamante V.(Sermaluc): Se modifica el método:
 * {@link #validarCalificacionVigente(long, long, RespuestaInvocacionesSolicitudTO[])}
 * Se agrega validación de monto solicitado y deuda. Si la solicitud no supera el valor paramétrico 
 * definido, no se realiza la validación. Se cambia instanciacion de Logger.</li>
 * <li>1.2 17/11/2014 Gonzalo Paredes C. (TINet): Se modifica el metodo
 * {@link #validarDatosBasicosEmpresario} para eliminar alertas que no corresponden a banca pyme.
 * <li>1.3 20/04/2015 Gonzalo Paredes C.(TINet) - Jessica Ramirez (ing. Soft. BCI):
 * Se modifica el metodo, para la omision de alertas de solicitudes con origen empresario.
 * <pre>
 *      {@link #validarDatosBasicosEmpresario}
 * </pre>
 * <li>1.4 04/04/2016 Desiree De Crescenzo R.(Sermaluc) - Patricio Valenzuela (Ing. Soft. BCI):
 * Se modifica el metodo, para incorporar reglas de negocio al validar atribuciones de garantía.
 * <pre>
 *      {@link #validarSolicitudEmpresario(long, String, RespuestaInvocacionesSolicitudTO[])
 * </pre>
 * <li>1.5 27/04/2016 Desiree De Crescenzo R.(Sermaluc) - Patricio Valenzuela (Ing. Soft. BCI):
 * Se agregan los siguientes metodos, para incorporar reglas de negocio correspondientes a Atribuciones de Garantía.
 * <pre>
 *      {@link #validarAtribucionGarantias(long, long, String, AtribucionColaboradorTO, RespuestaInvocacionesSolicitudTO[])}
 *      {@link #validarPerspectiva(long, long, CuadroDeMandoTO[], String, RespuestaInvocacionesSolicitudTO[])}
 *      {@link #validarCuentaCorriente(long, long, CuadroDeMandoTO[], int, RespuestaInvocacionesSolicitudTO[])}
 *      {@link #validarMarcaSeguimiento(long, long, CuadroDeMandoTO[], String, RespuestaInvocacionesSolicitudTO[])}
 *      {@link #validarFiltrosRiesgo(long, long, CuadroDeMandoTO[], String, RespuestaInvocacionesSolicitudTO[])}
 *      {@link #validarCalificacion(long, CuadroDeMandoTO[], String, double, RespuestaInvocacionesSolicitudTO[])}
 *      {@link #validarProbabilidadIncumplimiento(long, double)}
 * </pre>
 * <li>1.6 28/04/2016 Desiree De Crescenzo R.(Sermaluc) - Patricio Valenzuela (Ing. Soft. BCI):
 * Se modifica el metodo, para incorporar reglas de negocio al validar relacionas.
 * <pre>
 *      {@link #validarVigenciaRelaciones}validarVigenciaRelaciones(long rut, long numeroSolicitud,
        RespuestaInvocacionesSolicitudTO[] resultadoSolicitud)
 * </pre>
 * <li>1.7 10/06/2016 Desiree De Crescenzo R.(Sermaluc) - Patricio Valenzuela (Ing. Soft. BCI):
 * Se modifica el metodo:
 * <pre>
 *      {@link #validarVigenciaRelaciones(long rut, long numeroSolicitud,RespuestaInvocacionesSolicitudTO[] resultadoSolicitud)}
 * </pre>
 * <li>1.8 07/07/2016 Gonzalo Paredes C.(TINet) - Jessica Ramirez (ing. Soft. BCI): Se agregan datos a la
 * validacion de filtros de riesgo al aprobar, dado que faltaban los filtros de riesgo consultados en linea.
 * </li>
 * <li> 1.9 11/02/2017 Oscar Saavedra H. (TINet) - Patricio Valenzuela (ing. Soft. BCI): Se agrega logica de filtrado
 * para respuesta de motor.</li>
 * <li> 1.10 30/06/2017 Macarena Andrade, Desiree De Crescenzo R. (Sermaluc) - Patricio Valenzuela (ing. Soft. BCI): Se agrega método
 * <pre>
 * - {@link #validarAtribucionGarantias(long, String)}
 * </pre>
 * Se modifica el metodo para prueba:
 * <pre>
 * - {@link #validarDatosBasicosEmpresario(long, long, RespuestaInvocacionesSolicitudTO[])}
 * </pre>
 * </li>
 * </ul>
 * <p>
 * <b>Todos los derechos reservados por Banco de Crédito e Inversiones.</b>
 * <p>
 */
public class AlertasSolicitudBO {
    
    /**
     * Constante con el numero de meses de ventas.
     */
    private static final int CANTIDAD_MESES_VENTA = 12;

    /**
     * Constante con el valor del porcentaje.
     */
    private static final int CIEN_PORCIENTO = 100;
    
    /**
     * Constante con el valor para conversion de deuda a M$ (miles de pesos).
     */
    private static final int DIVISOR_MILES = 1000;
    
    /**
     * Indice del arreglo que contiene el valor del riesgo.
     */
    private static final int POSICION_VALOR_RIESGO = 1;
    
    /**
     * Indice del arreglo que contiene el valor del código de riesgo..
     */
    private static final int POSICION_COD_RIESGO = 0;
    
    /**
     * Constante con el valor del caracter comodin para ser reemplazado en los mensajes de alerta o error.
     */
    private static final String CARACTER_COMODIN = "{#}";
    
    /**
     * Indica si el cliente debe tener filtro de riesgo.
     */
    private static final String FILTRO_RIESGO = "SI";
    
    /**
     * Unidad de tiempo que indica los meses.
     */
    private static final char INDICADOR_MES = 'M';
    
    /**
     * Bandera temporal que indica si la existencia de un usuario debe ser validada en la base de datos.
     */
    private static final String VALIDAR_USUARIO = "S";
        
    /**
     * Valor que indica mensaje de error cuando el monto LCG solicitada excede al monto con y sin garantias.
     */    
    private static final String SUC_ATRIBUCION_GARANTIA_MTOEXCEDIDO = "SUC_ATRIBUCION_GARANTIA_MTOEXCEDIDO";
    
    /**
     * Valor que indica mensaje de error cuando el JOF no esta en la tabla ATRIBUCIONES_JOF.
     */
    private static final String SUC_ATRIBUCION_GARANTIA_USUARIO = "SUC_ATRIBUCION_GARANTIA_USUARIO";
    
    /**
     * Valor que indica mensaje de error cuando el monto LCG solicitada excede al monto sin garantias.
     */ 
    private static final String SUC_ATRIBUCION_GARANTIA_SINGARANTIA = "SUC_ATRIBUCION_GARANTIA_SINGARANTIA";
    
    /**
     * Valor que indica mensaje de error cuando el monto LCG solicitada excede al monto con garantias.
     */ 
    private static final String SUC_ATRIBUCION_GARANTIA_CONGARANTIA = "SUC_ATRIBUCION_GARANTIA_CONGARANTIA";
    
    /**
     * Valor que indica mensaje de error cuando el monto deficit excede al monto sin garantias.
     */ 
    private static final String SUC_ATRIBUCION_GARANTIA_MTODEFICITMAYOR = "SUC_ATRIBUCION_GARANTIA_MTODEFICIT";
    
    /**
     * Estado aprobación empresarios.
     */
    private static final String APROBAR_SOL_EMP = "APROBADA_EMP";
    

    /**
     * Logger de la clase.
     */
    private transient Logger logger = (Logger) Logger.getLogger(AlertasSolicitudBO.class);
    
    /**
     * Método get para logger de la clase.
     * <p>
     * Registro de versiones:
     * <ul>
     * <li>1.0 26/05/2014 Gonzalo Bustamante V.(Sermaluc): Versión inicial.</li>
     * <li>1.1 12/09/2014 Gonzalo Bustamante V.(Sermaluc): Se modifica instanciacion de logger.
     * </ul>
     * <p> 
     * @return Objeto de loggeo de la clase
     * @since 1.0
     */
    public Logger getLogger() {
        if (logger == null) {
            logger = Logger.getLogger(AlertasSolicitudBO.class);
        }
        return logger;
    }

    /**
     * Método encargado de registrar las alertas que son desplegadas en las solicitudes de crédito,
     * generadas en Banca Empresarios.
     * <p>
     * Registro de versiones:
     * <ul>
     * <li>1.0 26/05/2014 Gonzalo Bustamante V.(Sermaluc): Versión inicial.
     * </ul>
     * <p>
     * 
     * @param resultadoSolicitud
     *      Lista con mensajes de alertas para la solicitud de credito.
     * @param numeroSolicitud
     *      Numero identificador de la solicitud.
     * @throws GeneralException
     *      excepcion general
     * @since 1.0
     */
    private void guardarMensajesAlerta(long numeroSolicitud,
        RespuestaInvocacionesSolicitudTO[] resultadoSolicitud) throws GeneralException {
        if(this.getLogger().isEnabledFor(Level.INFO)){
            this.getLogger().info("[guardarMensajesAlerta] Iniciando metodo");
        }
        
        SolicitudesDAO solicitudesDao = new SolicitudesDAO();
        solicitudesDao.guardarMensajesAlerta(numeroSolicitud, resultadoSolicitud);
        
        if(this.getLogger().isEnabledFor(Level.INFO)){
            this.getLogger().info("[guardarMensajesAlerta] Fin metodo");
        }

    }

    /**
     * Método encargado de filtrar las alertas que no se desean mostrar para Banca Empresarios.
     * Se registran en base de datos las alertas que se van a desplegar.
     * <p>
     * Registro de versiones:
     * <ul>
     * <li>1.0 26/05/2014 Gonzalo Bustamante V.(Sermaluc): Versión inicial.
     * </ul>
     * <p> 
     * @param resultadoSolicitud
     *      Lista con mensajes de alertas para la solicitud de credito
     * @return lista de mensajes filtrados ya registrados en la base de datos
     * @since 1.0
     */
    private RespuestaInvocacionesSolicitudTO[] filtrarAlertasEmpresario(
        RespuestaInvocacionesSolicitudTO[] resultadoSolicitud) {

        if(this.getLogger().isEnabledFor(Level.INFO)){
            this.getLogger().info("[filtrarAlertasEmpresario] Iniciando metodo");
        }
        
        ArrayList mensajesAlerta = new ArrayList(0);
        String omisiones = TablaValores.getValor(SolicitudUtil.TABLA_SOLICITUDES,
            "ALERTAS_OMITIDAS", "valor");
        
        if(this.getLogger().isEnabledFor(Level.DEBUG)){
            this.getLogger().debug("[filtrarAlertasEmpresario] [alertas a omitir] = "
                + omisiones);
        }
        
        String[] alertasOmitidas = StringUtil.divide(omisiones, "#");
        
        if(alertasOmitidas == null || alertasOmitidas.length <= 0){
            if(this.getLogger().isEnabledFor(Level.DEBUG)){
                this.getLogger().debug("[filtrarAlertasEmpresario] No existen alertas para omitir");
            }
            return resultadoSolicitud;
        }

        boolean encontrada = false;
        
        if (resultadoSolicitud != null && resultadoSolicitud.length > 0) {
            for (int i = 0; i < resultadoSolicitud.length; i++) {
                RespuestaInvocacionesSolicitudTO mensaje = resultadoSolicitud[i];
                encontrada = false;
                for (int j = 0; j < alertasOmitidas.length; j++) {
                    if(mensaje.getCodigoMensaje().equalsIgnoreCase(alertasOmitidas[j])){
                        encontrada = true;
                        break;
                    }
                }
                if(!encontrada){
                    mensajesAlerta.add(mensaje);
                }
            }
            RespuestaInvocacionesSolicitudTO[] alertasFiltradas = new RespuestaInvocacionesSolicitudTO[
                mensajesAlerta.size()];
            alertasFiltradas = (RespuestaInvocacionesSolicitudTO[])mensajesAlerta.toArray(alertasFiltradas);
            
            if(this.getLogger().isEnabledFor(Level.DEBUG)){
                this.getLogger().debug("[filtrarAlertasEmpresario] Se aplicaron filtros de alertas");
            }
            return alertasFiltradas;
            
        }
        if(this.getLogger().isEnabledFor(Level.DEBUG)){
            this.getLogger().debug("[filtrarAlertasEmpresario] Lista de mensajes nula");
        }
        return null;
    }
    
    /**
     * Método encargado de validar las solicitudes de crédito generadas en Banca Empresarios.
     * Se filtran las alertas que no se desean mostrar para Banca Empresarios y se generan nuevas 
     * que aplican solo para esta banca.
     * Se registran en base de datos las alertas que se van a desplegar.
     * <p>
     * Registro de versiones:
     * <ul>
     * <li>1.0 26/05/2014 Gonzalo Bustamante V.(Sermaluc): Versión inicial.
     * <li>1.1 11/04/2016 Desiree De Crescenzo. (Sermaluc) - Patricio Valenzuela (Ing. Sw BCI): Se incorporan
     * validaciones correspondientes a las atribuciones de garantía.
     * <li>1.2 11/02/2017 Oscar Saavedra H. (TINet) - Patricio Valenzuela (ing. Soft. BCI): Se agrega logica de filtrado
     * para respuesta de motor.</li>
     * </ul>
     * <p>
     * @param numeroSolicitud numero de la solicitud.
     * @param codEjecutivo codigo del ejecutivo.
     * @param resultadoSolicitud Lista con mensajes de alertas para la solicitud de credito.
     * @param estadoACambiar Futuro estado de la solicitud.
     * @return lista de mensajes de alerts filtrados ya registrados en la base de datos.
     * @throws GeneralException excepcion general.
     * @since 1.0
     */
    public JustificacionSolicitudTO[] validarSolicitudEmpresario(long numeroSolicitud, String codEjecutivo,
        RespuestaInvocacionesSolicitudTO[] resultadoSolicitud, String estadoACambiar) throws GeneralException {
        if (this.getLogger().isEnabledFor(Level.INFO)) {
            this.getLogger().info(
                "[validarSolicitudEmpresario] Iniciando metodo para [solicitud=" + numeroSolicitud
                    + "]");
        }

        SolicitudesDAO solicitudesDAO = new SolicitudesDAO();
        SolicitudTO solicitud = null;
        long rut = 0;
        
        try {
            solicitud = solicitudesDAO.obtenerSolicitud(numeroSolicitud);
            if (solicitud != null) {
                rut = solicitud.getRutTitular();
            }
        }
        catch (SolicitudCreditoException e) {
            this.getLogger().error("[validarSolicitudEmpresario] No se logro obtener la solicitud: "
                + numeroSolicitud);
            throw new GeneralException(SolicitudCreditoException.ERROR_APLICACION);
        }

        if (this.getLogger().isEnabledFor(Level.DEBUG)) {
            this.getLogger().debug(
                "[validarSolicitud]Validando al titular de la solicitud [Rut = "
                    + rut + "]");
        }
        
        resultadoSolicitud = this.validarPatrimonio(rut, numeroSolicitud,
            resultadoSolicitud);

        resultadoSolicitud = this.validarClienteEmpresario(rut, codEjecutivo,
            numeroSolicitud, resultadoSolicitud);

        if (this.getLogger().isEnabledFor(Level.DEBUG)) {
            this.getLogger().debug("[validarSolicitud]Obteniendo integrantes solicitud");
        }
        IntegranteGrupoEconomicoTO[] integrantes = solicitudesDAO.obtenerGrupoClienteSolicitud(solicitud
            .getNumeroSolicitud());
               
        if (this.getLogger().isEnabledFor(Level.DEBUG)) {
            this.getLogger().debug("[validarSolicitud]Se van a validar los integrantes del grupo");
        }

        boolean esAutomatica = solicitud.getCanalAprobacion() != null && solicitud.getCanalAprobacion().equalsIgnoreCase(SolicitudUtil.AUTOMA);

        for (int i = 0; i < integrantes.length; i++) {
            if (!esAutomatica) {
                resultadoSolicitud = this.validarClienteEmpresario(integrantes[i].getRut(), codEjecutivo,
                    numeroSolicitud, resultadoSolicitud);
            }
            else if (integrantes[i].getAprobadoMotor() != null && (integrantes[i].getAprobadoMotor().equalsIgnoreCase("SI") 
                || integrantes[i].getAprobadoMotor().equalsIgnoreCase("S"))) {
            resultadoSolicitud = this.validarClienteEmpresario(integrantes[i].getRut(), codEjecutivo,
                numeroSolicitud, resultadoSolicitud);
        }
        }
        
        try {
            if (this.getLogger().isEnabledFor(Level.DEBUG)) {
                this.getLogger().debug("[validarSolicitud]Se filtran las alertas");
            }
            resultadoSolicitud = filtrarAlertasEmpresario(resultadoSolicitud);
        }
        catch (Exception e) {
            if(this.getLogger().isEnabledFor(Level.WARN)){
                this.getLogger().warn(
                    "[validarSolicitudEmpresario]Se produjo un error aplicando los filtros de alertas:"
                        + ErroresUtil.extraeStackTrace(e));
            }
        }
        ServiciosSolicitudesDelegate delegate = new ServiciosSolicitudesDelegate();
        
        String validoUsuario = TablaValores.getValor(SolicitudUtil.TABLA_SOLICITUDES,
            "VALIDAR_USUARIO", "valor");
        
        if (estadoACambiar != null && estadoACambiar.equals(APROBAR_SOL_EMP)) {
            
            if (this.getLogger().isEnabledFor(Level.DEBUG)) {
                this.getLogger().debug("[validarSolicitud]Validación de atribuciones");
            }
            AtribucionColaboradorTO atribucionColaboradorTO = delegate.obtenerAtribucionesGarantiaUsuario(codEjecutivo);
                
                if (atribucionColaboradorTO != null) {
                    try {
                        resultadoSolicitud =  this.validarAtribucionGarantias(rut, numeroSolicitud, codEjecutivo, atribucionColaboradorTO, resultadoSolicitud);
                        
                        OpcionListadoSolicitudTO[] parametro = (new ParametrosSolicitudDAO()).obtenerParametros("ATG", null);
                        
                        String marca="";
                        int antiguedadCuenta = 0;
                        String pi = "" ;
                        char tipoUsuario = 0;
                        String perspectiva = "";
                        String calificaciones = "";
                        String filtros = "";
                        
                        if (parametro != null && parametro.length > 0) {
                            for (int i = 0; i < parametro.length; i++) {
                                if (parametro[i].getValor().trim().equalsIgnoreCase("marca")){
                                    marca = parametro[i].getGlosa();
                                }
                                if (parametro[i].getValor().trim().equalsIgnoreCase("ANTCTA")) {
                                    antiguedadCuenta = Integer.parseInt(parametro[i].getGlosa());
                                }
                                if (parametro[i].getValor().trim().equalsIgnoreCase("PI")) {
                                    pi = parametro[i].getGlosa();
                                }
                                if (parametro[i].getValor().trim().equalsIgnoreCase("TIPOUSR")) {
                                    tipoUsuario = parametro[i].getGlosa().charAt(0);
                                }
                                if (parametro[i].getValor().trim().equalsIgnoreCase("RESPSP")) {
                                    perspectiva = parametro[i].getGlosa();
                                }
                                if (parametro[i].getValor().trim().equalsIgnoreCase("CALIF")) {
                                    calificaciones = parametro[i].getGlosa();
                                }
                                if (parametro[i].getValor().trim().equalsIgnoreCase("FILRIES")) {
                                    filtros = parametro[i].getGlosa();
                                }
                           }
                        }   
                        
                        if (atribucionColaboradorTO.getTipoColaborador() == tipoUsuario) {
                            if (this.getLogger().isEnabledFor(Level.DEBUG)) {
                                this.getLogger().debug("[validarSolicitudEmpresario]El usuario es " + tipoUsuario + " por lo cual se realizan validaciones extras.");
                            }
                            
                            EvaluacionSolicitudBO  evaluacionSolicitudBO = new EvaluacionSolicitudBO();
                            
                            ResolucionSolicitudTO datosResolucion;
                            try {
                                datosResolucion = evaluacionSolicitudBO.obtenerDatosResolucionDeSolicitud(numeroSolicitud, true,
                                    false, false);
                            }
                            catch (SolicitudCreditoException e) {
                                if (this.getLogger().isEnabledFor(Level.ERROR)) {
                                    this.getLogger().error("[validarSolicitudEmpresario] No se logro obtener los datos de resolución: "
                                    + numeroSolicitud);
                                }
                                throw new GeneralException(SolicitudCreditoException.ERROR_APLICACION);
                            }
                            
                            CuadroDeMandoTO[] datosSemaforo = evaluacionSolicitudBO.obtenerSemaforosEmprendedor(numeroSolicitud,
                                rut, datosResolucion.getCuadroDeMando());
                            
                            if (this.getLogger().isEnabledFor(Level.DEBUG)) {
                                this.getLogger().debug("[validarSolicitudEmpresario]Se va a validar la cuenta corriente");
                            }
                            
                            resultadoSolicitud = this.validarCuentaCorriente(rut, numeroSolicitud, datosSemaforo, antiguedadCuenta, resultadoSolicitud);
                            
                            if (this.getLogger().isEnabledFor(Level.DEBUG)) {
                                this.getLogger().debug("[validarSolicitudEmpresario]Se valida Marca de Seguimiento");
                            }
                            
                            resultadoSolicitud = this.validarMarcaSeguimiento(rut, numeroSolicitud, datosSemaforo, marca, resultadoSolicitud);
                            
                            if (this.getLogger().isEnabledFor(Level.DEBUG)) {
                                this.getLogger().debug("[validarSolicitudEmpresario]Se valida Perspectiva");
                            }
                            
                            resultadoSolicitud = this.validarPerspectiva(rut, numeroSolicitud, datosSemaforo, perspectiva, resultadoSolicitud);
                            
                            if (this.getLogger().isEnabledFor(Level.DEBUG)) {
                                this.getLogger().debug("[validarSolicitudEmpresario]Se valida Filtros de Riesgo duro y/o blando.");
                            }
                            
                            resultadoSolicitud = this.validarFiltrosRiesgo(rut, numeroSolicitud, datosSemaforo, filtros, resultadoSolicitud);
                           
                            if (this.getLogger().isEnabledFor(Level.DEBUG)) {
                                this.getLogger().debug("[validarSolicitudEmpresario]Se valida Clientes con PI o clasificacion individual con valores según parámetros.");
                            }
                            resultadoSolicitud = this.validarCalificacion(numeroSolicitud, datosSemaforo, calificaciones, Double.parseDouble(pi), resultadoSolicitud);
                 
                        }
                    }
                    catch (SolicitudCreditoException e) {
                        if (this.getLogger().isEnabledFor(Level.ERROR)) {
                            this.getLogger().error("[validarSolicitudEmpresario] No se logro obtener validar las atribuciones de garantia: "
                            + numeroSolicitud);
                        }
                        throw new GeneralException(SolicitudCreditoException.ERROR_APLICACION);
                    }
                }
                else {
                    if (validoUsuario.equalsIgnoreCase(AlertasSolicitudBO.VALIDAR_USUARIO)) {
                        throw new GeneralException(SUC_ATRIBUCION_GARANTIA_USUARIO); 
                    }  
                }    
        }

        try{
            if (this.getLogger().isEnabledFor(Level.DEBUG)) {
                this.getLogger().debug("[validarSolicitud]Se guardan las alertas en base de datos");
            }
            guardarMensajesAlerta(numeroSolicitud, resultadoSolicitud);
        }
        catch(Exception e){
            this.getLogger().error(
                "[validarSolicitudEmpresario]Se produjo un error al guardar alertas en base datos:"
                    + ErroresUtil.extraeStackTrace(e));
            throw new GeneralException("SA-001", "Error al registrar alertas en base datos");
        }
        
        if (this.getLogger().isEnabledFor(Level.DEBUG)) {
            this.getLogger().debug("[validarSolicitudEmpresario] Fin metodo");
        }

        return transformarTOJustificacion(resultadoSolicitud);
    }
    
    /**
     * Método encargado de realizar las validaciones para despliegue de alerta.
     * <p>
     * Registro de versiones:
     * <ul>
     * <li>1.0 28/05/2014 Gonzalo Bustamante V.(Sermaluc): Versión inicial.
     * </ul>
     * <p>
     * @param rut
     *      Rut a consultar
     * @param codEjecutivo
     *      codigo del ejecutivo
     * @param numeroSolicitud 
     *      numero de la solicitud
     * @param resultadoSolicitud
     *      Lista con mensajes de alertas para la solicitud de credito
     * @return lista de mensajes de alerta
     * @throws GeneralException
     *      excepcion general
     * @since 1.0
     */
    private RespuestaInvocacionesSolicitudTO[] validarClienteEmpresario(long rut, String codEjecutivo,
        long numeroSolicitud, RespuestaInvocacionesSolicitudTO[] resultadoSolicitud)
        throws GeneralException {
        
        if (this.getLogger().isEnabledFor(Level.INFO)) {
            this.getLogger().info(
                "[validarClienteEmpresario] Iniciando metodo, con parametros [RUT = " + rut
                    + "][EJECUTIVO = " + codEjecutivo + "][Solicitud = " + numeroSolicitud
                    + "].");
        }
        resultadoSolicitud = this.validarGastoFinanciero(rut, numeroSolicitud, resultadoSolicitud);

        resultadoSolicitud = this.validarEbitda(rut, numeroSolicitud, resultadoSolicitud);

        resultadoSolicitud = this.validarTasaRiesgoIntegral(rut, numeroSolicitud,
            resultadoSolicitud);

        resultadoSolicitud = this.validarBalancesCliente(rut, numeroSolicitud, resultadoSolicitud);

        resultadoSolicitud = this.validarInconsistenciasVentas(rut, numeroSolicitud,
            resultadoSolicitud);

        resultadoSolicitud = this.validarVentas(rut, numeroSolicitud, resultadoSolicitud);

        resultadoSolicitud = this.validarCalificacionVigente(rut, numeroSolicitud,
            resultadoSolicitud);

        resultadoSolicitud = this.validarDatosBasicosEmpresario(rut, numeroSolicitud,
            resultadoSolicitud);

        resultadoSolicitud = this.validarRIB(rut, numeroSolicitud, codEjecutivo, resultadoSolicitud);
        
        resultadoSolicitud = this.validarVigenciaRelaciones(rut, numeroSolicitud, resultadoSolicitud);
        
        if (this.getLogger().isEnabledFor(Level.INFO)) {
            this.getLogger().info(
                "[validarClienteEmpresario] Fin validacion cliente.");
        }
        
        return resultadoSolicitud;
    }

    /**
     * Método encargado de realizar la validación del patrimonio grupo para despliegue de alerta.
     * <p>
     * Registro de versiones:
     * <ul>
     * <li>1.0 28/05/2014 Gonzalo Bustamante V.(Sermaluc): Versión inicial.
     * </ul>
     * <p>
     * @param numeroSolicitud 
     *      numero de la solicitud
     * @param resultadoSolicitud
     *      Lista con mensajes de alertas para la solicitud de credito
     * @param rut
     *      Rut a consultar
     * @return lista de mensajes de alerta
     * @since 1.0
     */
    private RespuestaInvocacionesSolicitudTO[] validarPatrimonio(long rut, long numeroSolicitud,
        RespuestaInvocacionesSolicitudTO[] resultadoSolicitud) {

        if (this.getLogger().isEnabledFor(Level.INFO)) {
            this.getLogger().info("[validarPatrimonio] Iniciando metodo");
        }

        try {

            if (this.getLogger().isEnabledFor(Level.DEBUG)) {
                this.getLogger().debug("[validarPatrimonio] Obteniendo datos de la solicitud");
            }

            EvaluacionSolicitudBO evaluacionBO = new EvaluacionSolicitudBO();

            if (this.getLogger().isEnabledFor(Level.DEBUG)) {
                this.getLogger().debug("[validarPatrimonio] Obteniendo valor del patrimonio grupo");
            }
            double patrimonioGrupo = evaluacionBO.obtenerSemaforoPatrimonioGrupo(numeroSolicitud,
                rut);

            if (patrimonioGrupo < 0) {
                if (this.getLogger().isEnabledFor(Level.DEBUG)) {
                    this.getLogger().debug(
                        "[validarPatrimonio] Patrimonio grupo negativo, se añade mensaje");
                }
                resultadoSolicitud = agregaMensaje(rut, numeroSolicitud, "VALIDA_PATRIMONIO_GRUPO",
                    resultadoSolicitud, null);

            }
            if (this.getLogger().isEnabledFor(Level.INFO)) {
                this.getLogger().info("[validarPatrimonio] Fin metodo");
            }
        }
        catch (Exception e) {
            if(this.getLogger().isEnabledFor(Level.WARN)){
                this.getLogger().warn(
                    "[validarPatrimonio]Se produjo un error al obtener alerta patrimonio:"
                        + ErroresUtil.extraeStackTrace(e));
            }
        }
        return resultadoSolicitud;
    }
    
    /**
     * Método encargado de realizar la validación de si el Gasto financiero excede el 10% del
     * resultado operacional, para despliegue de alerta.
     * <p>
     * Registro de versiones:
     * <ul>
     * <li>1.0 28/05/2014 Gonzalo Bustamante V.(Sermaluc): Versión inicial.
     * </ul>
     * <p>
     * @param numeroSolicitud 
     *      numero de la solicitud
     * @param resultadoSolicitud
     *      Lista con mensajes de alertas para la solicitud de credito
     * @param rut
     *      Rut a consultar
     * @return lista de mensajes de alerta
     * @since 1.0
     */
    private RespuestaInvocacionesSolicitudTO[] validarGastoFinanciero(long rut,
        long numeroSolicitud, RespuestaInvocacionesSolicitudTO[] resultadoSolicitud) {

        if (this.getLogger().isEnabledFor(Level.INFO)) {
            this.getLogger().info("[validarGastoFinanciero] Iniciando metodo");
        }

        try {
            SolicitudesDAO dao = new SolicitudesDAO();

            if (this.getLogger().isEnabledFor(Level.DEBUG)) {
                this.getLogger()
                    .debug("[validarGastoFinanciero] se obtiene el valor del indicador");
            }
            double gastoFinanciero = dao.obtenerSemaforoResultadoOperacionalEmprendedor(
                numeroSolicitud, rut);

            int valorLimite = Integer.parseInt(TablaValores.getValor(
                SolicitudUtil.TABLA_SOLICITUDES, "RESULTADO_OPERACIONAL", "valor"));

            if (gastoFinanciero > valorLimite) {

                if (this.getLogger().isEnabledFor(Level.DEBUG)) {
                    this.getLogger().debug(
                        "[validarGastoFinanciero] Resultado mayor al " + valorLimite
                            + "%. Se añade mensaje");
                }
                resultadoSolicitud = agregaMensaje(rut, numeroSolicitud,
                    "VALIDA_GASTO_FINANCIERO_RESULTADO_OPERACIONAL", resultadoSolicitud, null);
            }

            if (this.getLogger().isEnabledFor(Level.INFO)) {
                this.getLogger().info("[validarGastoFinanciero] Fin metodo");
            }
        }
        catch (Exception e) {
            if(this.getLogger().isEnabledFor(Level.WARN)){
                this.getLogger().warn(
                    "[validarPatrimonio]Se produjo un error al obtener alerta gasto operacional:"
                        + ErroresUtil.extraeStackTrace(e));
            }
        }
        return resultadoSolicitud;
    }
    
    /**
     * Método encargado de realizar la validación del EBITDA, para despliegue de alerta.
     * <p>
     * Registro de versiones:
     * <ul>
     * <li>1.0 28/05/2014 Gonzalo Bustamante V.(Sermaluc): Versión inicial.
     * </ul>
     * <p>
     * @param numeroSolicitud 
     *      numero de la solicitud
     * @param resultadoSolicitud
     *      Lista con mensajes de alertas para la solicitud de credito
     * @param rut
     *      Rut a consultar
     * @return lista de mensajes de alerta
     * @since 1.0
     */
    private RespuestaInvocacionesSolicitudTO[] validarEbitda(long rut, long numeroSolicitud,
        RespuestaInvocacionesSolicitudTO[] resultadoSolicitud) {
        if (this.getLogger().isEnabledFor(Level.INFO)) {
            this.getLogger().info("[validarEbitda] Iniciando metodo");
        }

        try {
            SolicitudesDAO dao = new SolicitudesDAO();

            if (this.getLogger().isEnabledFor(Level.DEBUG)) {
                this.getLogger().debug("[validarEbitda] se obtiene el valor del indicador");
            }
            double ebitda = dao.obtenerSemaforoEbitda(numeroSolicitud, rut);

            int valorLimite = Integer.parseInt(TablaValores.getValor(
                SolicitudUtil.TABLA_SOLICITUDES, "EBITDA", "valor"));

            if (ebitda < valorLimite) {

                if (this.getLogger().isEnabledFor(Level.DEBUG)) {
                    this.getLogger().debug(
                        "[validarEbitda] Resultado menor al valor de corte " + valorLimite
                            + ". Se añade mensaje");
                }
                resultadoSolicitud = agregaMensaje(rut, numeroSolicitud, "VALIDA_EBITDA",
                    resultadoSolicitud, null);
            }
        }
        catch (Exception e) {
            if(this.getLogger().isEnabledFor(Level.WARN)){
                this.getLogger().warn(
                    "[validarEbitda]Se produjo un error al obtener alerta EBITDA:"
                        + ErroresUtil.extraeStackTrace(e));
            }
        }
        if (this.getLogger().isEnabledFor(Level.INFO)) {
            this.getLogger().info("[validarEbitda] Fin metodo");
        }
        return resultadoSolicitud;
    }
    
    /**
     * Método encargado de realizar la validación la tasa de riesgo integral, para despliegue de alerta.
     * <p>
     * Registro de versiones:
     * <ul>
     * <li>1.0 28/05/2014 Gonzalo Bustamante V.(Sermaluc): Versión inicial.
     * </ul>
     * <p>
     * @param numeroSolicitud 
     *      numero de la solicitud
     * @param resultadoSolicitud
     *      Lista con mensajes de alertas para la solicitud de credito
     * @param rut
     *      Rut a consultar
     * @return lista de mensajes de alerta
     * @since 1.0
     */
    private RespuestaInvocacionesSolicitudTO[] validarTasaRiesgoIntegral(long rut,
        long numeroSolicitud, RespuestaInvocacionesSolicitudTO[] resultadoSolicitud) {
        if (this.getLogger().isEnabledFor(Level.INFO)) {
            this.getLogger().info("[validarTasaRiesgoIntegral] Iniciando metodo");
        }

        try {
            SolicitudesDAO dao = new SolicitudesDAO();

            if (this.getLogger().isEnabledFor(Level.DEBUG)) {
                this.getLogger().debug(
                    "[validarTasaRiesgoIntegral] se obtiene el valor del indicador");
            }
            double tasaRiesgo = dao.obtenerSemaforoTasaRiesgoIntegralEmprendedor(numeroSolicitud,
                rut);

            int valorLimite = Integer.parseInt(TablaValores.getValor(
                SolicitudUtil.TABLA_SOLICITUDES, "TASA_RIESGO", "valor"));

            if (tasaRiesgo > valorLimite) {

                if (this.getLogger().isEnabledFor(Level.DEBUG)) {
                    this.getLogger().debug(
                        "[validarTasaRiesgoIntegral] Resultado menor al valor de corte "
                            + valorLimite + ". Se añade mensaje");
                }
                resultadoSolicitud = agregaMensaje(rut, numeroSolicitud,
                    "VALIDA_TASA_RIESGO_INTEGRAL", resultadoSolicitud, null);
            }
        }
        catch (Exception e) {
            if(this.getLogger().isEnabledFor(Level.WARN)){
                this.getLogger().warn(
                    "[validarTasaRiesgoIntegral]Se produjo un error al obtener alerta Tasa de Riesgo:"
                        + ErroresUtil.extraeStackTrace(e));
            }
        }

        if (this.getLogger().isEnabledFor(Level.INFO)) {
            this.getLogger().info("[validarTasaRiesgoIntegral] Fin metodo");
        }
        return resultadoSolicitud;
    }
    
    /**
     * Método encargado de realizar la validación RIB, para despliegue de alerta.
     * <p>
     * Registro de versiones:
     * <ul>
     * <li>1.0 28/05/2014 Gonzalo Bustamante V.(Sermaluc): Versión inicial.
     * </ul>
     * <p>
     * @param rut
     *      Rut a consultar
     * @param numeroSolicitud 
     *      numero de la solicitud
     * @param codEjecutivo
     *      codigo del ejecutivo
     * @param resultadoSolicitud
     *      Lista con mensajes de alertas para la solicitud de credito
     * @return lista de mensajes de alerta
     * @since 1.0
     */
    private RespuestaInvocacionesSolicitudTO[] validarRIB(long rut, long numeroSolicitud, String codEjecutivo,
        RespuestaInvocacionesSolicitudTO[] resultadoSolicitud) {
        try{
            if (this.getLogger().isEnabledFor(Level.INFO)) {
                this.getLogger().info("[validarRIB] Iniciando metodo");
            }
            
            SolicitudesDAO dao = new SolicitudesDAO();
            
            if (this.getLogger().isEnabledFor(Level.DEBUG)) {
                this.getLogger().debug("[validarRIB] Se obtiene el analisis y las observaciones");
            }
            AnalisisSolicitudTO analisis = dao.obtenerAnalisisSolicitud(numeroSolicitud, codEjecutivo);
            
            ObservacionAnalisisSolicitudTO[] observaciones = dao.obtenerObservacionesAnalisis(analisis
                .getIdAnalisis());
            
            if(observaciones != null && observaciones.length > 0){
                boolean sinObservacion = false;
                
                for (int i = 0; i < observaciones.length; i++) {
                    if (observaciones[i].getObservacion() == null
                        || observaciones[i].getObservacion().equalsIgnoreCase("")) {
                        sinObservacion = true;
                        break;
                    }
                }
                
                if (sinObservacion) {
                    if (this.getLogger().isEnabledFor(Level.DEBUG)) {
                        this.getLogger().debug(
                            "[validarRIB] La solicitud posee observaciones, pero estan incompletas");
                    }
                    resultadoSolicitud = agregaMensaje(rut, numeroSolicitud, "VALIDA_RIB",
                        resultadoSolicitud, null);
                }
            }
            else{
                if (this.getLogger().isEnabledFor(Level.DEBUG)) {
                    this.getLogger().debug("[validarRIB] La solicitud no posee observaciones");
                }
                resultadoSolicitud = agregaMensaje(rut, numeroSolicitud, "VALIDA_RIB",
                    resultadoSolicitud, null);
            }
        }
        catch(Exception e){
            if(this.getLogger().isEnabledFor(Level.WARN)){
                this.getLogger().warn(
                    "[validarRIB] Error al validar el RIB de la solicitud: " + ErroresUtil.extraeStackTrace(e));
            }
        }
        return resultadoSolicitud;
    }

    /**
     * Método encargado de realizar la validación de datos basicos de empresario, para despliegue de alerta.
     * <p>
     * Registro de versiones:
     * <ul>
     * <li>1.0 28/05/2014 Gonzalo Bustamante V.(Sermaluc): Versión inicial.
     * </li>
     * <li>1.1 17/11/2014 Gonzalo Paredes C. (TINet): Se eliminan alertas cuando la banca del cliente sea pyme.
     * <li>1.2 20/04/2015 Gonzalo Paredes C.(TINet) - Jessica Ramirez (ing. Soft. BCI):
     * Se modifica validacion, para omitir la solicitud de datos basicos a las solicitudes de origen empresario.
     * <li>1.3 24/07/2017 Desiree De Crescenzo R.(Sermaluc) - Patricio Valenzuela (ing. Soft. BCI):
     * Se modifica validacion, para omitir la solicitud de datos basicos a las solicitudes con categoría Persona Natural. 
     * </ul>
     * <p>
     * @param numeroSolicitud 
     *      numero de la solicitud
     * @param resultadoSolicitud
     *      Lista con mensajes de alertas para la solicitud de credito
     * @param rut
     *      Rut a consultar
     * @return lista de mensajes de alerta
     * @since 1.0
     */
    private RespuestaInvocacionesSolicitudTO[] validarDatosBasicosEmpresario(long rut,
        long numeroSolicitud, RespuestaInvocacionesSolicitudTO[] resultadoSolicitud) {
        if (this.getLogger().isEnabledFor(Level.INFO)) {
            this.getLogger().info("[validarDatosBasicosEmpresario][BCI_INI] Iniciando metodo");
        }
        try{
            wcorp.model.actores.Cliente utilitarioCliente = null;
            Cliente cliente = null;
            boolean esPersona = false;

            utilitarioCliente = new wcorp.model.actores.Cliente(rut, RUTUtil.calculaDigitoVerificador(rut));
            cliente = utilitarioCliente.getCliente();
            esPersona = (cliente instanceof ClientePersona ? true : false);
            
            DatosBasicosSolicitudDAO dao = new DatosBasicosSolicitudDAO();
            SolicitudTO solicitudTO = new SolicitudTO();
            SolicitudesDAO daoSolicitudes = new SolicitudesDAO();
            
            solicitudTO = daoSolicitudes.obtenerSolicitud(numeroSolicitud);
            String tipoCliente = daoSolicitudes.obtenerTipoCliente(numeroSolicitud,rut);
            
            if (tipoCliente == null) {
                if (this.getLogger().isEnabledFor(Level.DEBUG)) {
                    this.getLogger().debug(
                        "[validarDatosBasicosEmpresario] No existe la categoría del cliente");
                }
                resultadoSolicitud = agregaMensaje(rut, numeroSolicitud,
                    "VALIDA_DATOS_BASICOS_EMPRESARIO", resultadoSolicitud, null);
                
                return resultadoSolicitud;
            }
            
            boolean faltanCampos = false;
            boolean origenEmp = SolicitudUtil.esOrigenEmpresario(solicitudTO.getOrigen());
                      
            if(esPersona){
                if (origenEmp && tipoCliente.equals("PERNAT")) {
                    if (this.getLogger().isEnabledFor(Level.DEBUG)) {
                        this.getLogger().debug(
                            "[validarDatosBasicosEmpresario] No se valida por ser origen emp y Persona Natural");
                    }
                }
                else {
                    if (this.getLogger().isEnabledFor(Level.DEBUG)) {
                        this.getLogger().debug(
                            "[validarDatosBasicosEmpresario] se obtienen datos basicos persona");
                    }
                    
                    ClientePersona clientePersona = null;
                    solicitudTO.setRutTitular(rut);                    
                    DatosDelClienteVO datosDelClienteVO = dao.obtenerDatosBasicosClienteSolicitudEmpresario(solicitudTO);
                    
                    if (datosDelClienteVO != null) {
                        if(datosDelClienteVO.getClientePersona() != null){
                            clientePersona = datosDelClienteVO.getClientePersona();
                            if (clientePersona.getSectorEconomico() == null
                                || clientePersona.getSectorEconomico().equalsIgnoreCase("")
                                || clientePersona.getSectorEconomico().equalsIgnoreCase("-1")) {
                                faltanCampos = true;
                            }
                            else if (clientePersona.getAntiguedadGiro() == null
                                || clientePersona.getAntiguedadGiro().equalsIgnoreCase("")
                                || clientePersona.getAntiguedadGiro().equalsIgnoreCase("-1")) {
                                faltanCampos = true;
                            }
                            else if (clientePersona.getTipoAdministracion() == null
                                || clientePersona.getTipoAdministracion().equalsIgnoreCase("")
                                || clientePersona.getTipoAdministracion().equalsIgnoreCase("-1")) {
                                faltanCampos = true;
                            }
                            if(faltanCampos){
                                if (this.getLogger().isEnabledFor(Level.DEBUG)) {
                                    this.getLogger().debug(
                                        "[validarDatosBasicosEmpresario] Datos incompletos");
                                }
                                resultadoSolicitud = agregaMensaje(rut, numeroSolicitud,
                                    "VALIDA_DATOS_BASICOS_EMPRESARIO", resultadoSolicitud, null);
                            }
                        }
                    }
                }
                return resultadoSolicitud;
            }
            
            if (this.getLogger().isEnabledFor(Level.DEBUG)) {
                this.getLogger().debug(
                    "[validarDatosBasicosEmpresario] Se obtienen los datos basicos de empresario");
            }
            DatosEmpresa datosBasicos = dao.obtenerDatosBasicosEmpresario(rut, numeroSolicitud);
            
            if(datosBasicos != null){
                
                if (datosBasicos.getSectorEconomico() == null
                    || datosBasicos.getSectorEconomico().equalsIgnoreCase("")
                    || datosBasicos.getSectorEconomico().equalsIgnoreCase("-1")) {
                    faltanCampos = true;
                }
                else if (datosBasicos.getAntiguedadGiro() == null
                    || datosBasicos.getAntiguedadGiro().equalsIgnoreCase("")
                    || datosBasicos.getAntiguedadGiro().equalsIgnoreCase("-1")) {
                    faltanCampos = true;
                }
                else if (datosBasicos.getTipoAdministracion() == null
                    || datosBasicos.getTipoAdministracion().equalsIgnoreCase("")
                    || datosBasicos.getTipoAdministracion().equalsIgnoreCase("-1")) {
                    faltanCampos = true;
                }
                
                if(faltanCampos){
                    if (this.getLogger().isEnabledFor(Level.DEBUG)) {
                        this.getLogger().debug(
                            "[validarDatosBasicosEmpresario] Datos incompletos");
                    }
                    resultadoSolicitud = agregaMensaje(rut, numeroSolicitud,
                        "VALIDA_DATOS_BASICOS_EMPRESARIO", resultadoSolicitud, null);
                }
                
            }
            else{
                if (this.getLogger().isEnabledFor(Level.DEBUG)) {
                    this.getLogger().debug(
                        "[validarDatosBasicosEmpresario] No existen datos basicos");
                }
                resultadoSolicitud = agregaMensaje(rut, numeroSolicitud,
                    "VALIDA_DATOS_BASICOS_EMPRESARIO", resultadoSolicitud, null);
            }
        }
        catch (Exception e) {
            if(this.getLogger().isEnabledFor(Level.WARN)){
                this.getLogger().warn(
                    "[validarDatosBasicosEmpresario] Error al obtener validacion de datos basicos empresario: "
                        + ErroresUtil.extraeStackTrace(e));
            }
        }

        if (getLogger().isEnabledFor(Level.DEBUG)) {
            getLogger().debug("[validarDatosBasicosEmpresario][BCI_FINOK] Fin metodo.");
        }

        return resultadoSolicitud;
    }

    /**
     * Método encargado de realizar la validación calificacion vigente, para despliegue de alerta.
     * <p>
     * Registro de versiones:
     * <ul>
     * <li>1.0 28/05/2014 Gonzalo Bustamante V.(Sermaluc): Versión inicial.</li>
     * <li>1.1 12/09/2014 Gonzalo Bustamante V.(Sermaluc): Se agrega validación de monto solicitado y deuda.
     * Si la solicitud no supera el valor paramétrico definido, no se realiza la validación.</li>
     * </ul>
     * <p>
     * @param numeroSolicitud 
     *      numero de la solicitud
     * @param resultadoSolicitud
     *      Lista con mensajes de alertas para la solicitud de credito
     * @param rut
     *      Rut a consultar
     * @return lista de mensajes de alerta
     * @since 1.0
     */
    private RespuestaInvocacionesSolicitudTO[] validarCalificacionVigente(long rut,
        long numeroSolicitud, RespuestaInvocacionesSolicitudTO[] resultadoSolicitud) {
        
        if (this.getLogger().isEnabledFor(Level.INFO)) {
            this.getLogger().info("[validarCalificacionVigente] Iniciando metodo para el [rut = "
                    + rut +"] y [solicitud = " + numeroSolicitud + "]");
        }
        try {
            
            SolicitudesDAO solicitudesDao = new SolicitudesDAO();
            
            int montoExigibleCalificacion = 0;
            int montoAdvertenciaCalificacion = 0;
            
            montoExigibleCalificacion= Integer.parseInt(TablaValores.getValor(
                SolicitudUtil.TABLA_SOLICITUDES, "MONTO_MAXIMO_CRITICO", "valor"));
            montoAdvertenciaCalificacion = Integer.parseInt(TablaValores.getValor(
                SolicitudUtil.TABLA_SOLICITUDES, "MONTO_MAXIMO_ADVERTENCIA", "valor"));
            
            if (this.getLogger().isEnabledFor(Level.DEBUG)) {
                this.getLogger().debug(
                    "[validarCalificacionVigente] Obteniendo deuda [cliente = " + rut + "]");
            }
            
            double deudaCalificacion = solicitudesDao.obtenerDeudaClienteCalificacion(rut);
            deudaCalificacion = deudaCalificacion / DIVISOR_MILES;
            
            double montoSolicitado = 0;
            
            if (this.getLogger().isEnabledFor(Level.DEBUG)) {
                this.getLogger().debug(
                    "[validarCalificacionVigente] Obteniendo Monto solicitado [cliente = " + rut + "]");
            }
            CuadroDeMandoTO[] cuadrosMando = solicitudesDao.obtenerCuadroDeMando(numeroSolicitud);
            
            if(cuadrosMando!= null){
                int j = 0;
                while(cuadrosMando[j].getRutSolicitante() != rut) j++;
                montoSolicitado = cuadrosMando[j].getMontoLineaDeCreditoGlobal();
            }
            
            if (this.getLogger().isEnabledFor(Level.DEBUG)) {
                this.getLogger().debug(
                    "[validarCalificacionVigente] validando montos contra valor de corte. [DEUDA = "
                        + deudaCalificacion +"][SOLICITADO = " + montoSolicitado + "]");
            }
            
            if(deudaCalificacion > montoExigibleCalificacion || montoSolicitado > montoExigibleCalificacion){
                if (this.getLogger().isEnabledFor(Level.DEBUG)) {
                    this.getLogger().debug(
                        "[validarCalificacionVigente] Deuda o monto solicitado superior a valor de alerta "
                            + "critica. Se validan calificaciones");
                }
            InfoHistoriaCalificacionTO[] calificaciones = null;
            CalificacionDeClientesDelegate delegate = new CalificacionDeClientesDelegate();

            calificaciones = delegate.obtenerUltimasCalificaciones(Long.toString(rut));

            if(calificaciones != null && calificaciones.length > 0){
                int antiguedadVigencia = Integer.parseInt(TablaValores.getValor(
                        SolicitudUtil.TABLA_SOLICITUDES, "ANT_CALIFICACION", "valor_vencida"));
                Date fechaActual = new Date();
                boolean existeVigente = false;
                for (int i = 0; i < calificaciones.length; i++) {
                    if (calificaciones[i].getFechaHito() != null
                        && FechasUtil.diferenciaDeMeses(calificaciones[i].getFechaHito(),
                            fechaActual) <= antiguedadVigencia) {
                        existeVigente = true;
                        break;
                    }
                }
                if (!existeVigente) {
                    if (this.getLogger().isEnabledFor(Level.DEBUG)) {
                        this.getLogger().debug(
                            "[validarCalificacionVigente] Cliente no posee calificacion vigente");
                    }
                    resultadoSolicitud = agregaMensaje(rut, numeroSolicitud,
                            "VALIDA_CALIFICACION_VENCIDA_CRITICO", resultadoSolicitud, null);
                }
            }
            else{
                if (this.getLogger().isEnabledFor(Level.DEBUG)) {
                    this.getLogger().debug("[validarCalificacionVigente] Cliente no posee calificacion");
                }
                resultadoSolicitud = agregaMensaje(rut, numeroSolicitud,
                        "VALIDA_CALIFICACION_VENCIDA_CRITICO", resultadoSolicitud, null);
                }
            }
            else if (deudaCalificacion > montoAdvertenciaCalificacion
                || montoSolicitado > montoAdvertenciaCalificacion) {
                if (this.getLogger().isEnabledFor(Level.DEBUG)) {
                    this.getLogger().debug(
                        "[validarCalificacionVigente] Deuda o monto solicitado superior a valor de alerta "
                            + " no critica, pero inferior a alerta critica. Se validan calificaciones");
                }
                
                InfoHistoriaCalificacionTO[] calificaciones = null;
                CalificacionDeClientesDelegate delegate = new CalificacionDeClientesDelegate();

                calificaciones = delegate.obtenerUltimasCalificaciones(Long.toString(rut));
                
                if(calificaciones != null && calificaciones.length > 0){
                    int antiguedadVigencia = Integer.parseInt(TablaValores.getValor(
                        SolicitudUtil.TABLA_SOLICITUDES, "ANT_CALIFICACION", "valor_vencida"));
                    Date fechaActual = new Date();
                    boolean existeVigente = false;
                    for (int i = 0; i < calificaciones.length; i++) {
                        if (calificaciones[i].getFechaHito() != null
                            && FechasUtil.diferenciaDeMeses(calificaciones[i].getFechaHito(),
                                fechaActual) <= antiguedadVigencia) {
                            existeVigente = true;
                            break;
                        }
                    }
                    if (!existeVigente) {
                        if (this.getLogger().isEnabledFor(Level.DEBUG)) {
                            this.getLogger().debug(
                                "[validarCalificacionVigente] Cliente no posee calificacion vigente");
                        }
                        resultadoSolicitud = agregaMensaje(rut, numeroSolicitud,
                            "VALIDA_CALIFICACION_VENCIDA_ADVERTENCIA", resultadoSolicitud, null);
                    }
                }
                else{
                    if (this.getLogger().isEnabledFor(Level.DEBUG)) {
                        this.getLogger().debug("[validarCalificacionVigente] Cliente no posee calificacion");
                    }
                    resultadoSolicitud = agregaMensaje(rut, numeroSolicitud,
                        "VALIDA_CALIFICACION_VENCIDA_ADVERTENCIA", resultadoSolicitud, null);
                }
                
            }
            else{
                if (this.getLogger().isEnabledFor(Level.DEBUG)) {
                    this.getLogger().debug(
                        "[validarCalificacionVigente] La deuda y el monto solicitado no son suficientes para "
                            + "validacion de calificaciones");
                }
            }
        }
        catch (Exception e) {
            if(this.getLogger().isEnabledFor(Level.WARN)){
                this.getLogger().warn(
                    "[validarCalificacionVigente] Error al obtener calificaciones cliente: "
                        + ErroresUtil.extraeStackTrace(e));
            }
        }
        return resultadoSolicitud;
    }

    /**
     * Método encargado de realizar la validación de las ventas, para despliegue de alerta.
     * <p>
     * Registro de versiones:
     * <ul>
     * <li>1.0 28/05/2014 Gonzalo Bustamante V.(Sermaluc): Versión inicial.
     * </ul>
     * <p>
     * @param numeroSolicitud 
     *      numero de la solicitud
     * @param resultadoSolicitud
     *      Lista con mensajes de alertas para la solicitud de credito
     * @param rut
     *      Rut a consultar
     * @return lista de mensajes de alerta
     * @since 1.0
     */
    private RespuestaInvocacionesSolicitudTO[] validarVentas(long rut, long numeroSolicitud,
        RespuestaInvocacionesSolicitudTO[] resultadoSolicitud) {

        if (this.getLogger().isEnabledFor(Level.INFO)) {
            this.getLogger().info("[validarVentas] Iniciando metodo");
        }
        try {
            EstadoFinancieroYVentasSolicitudDAO dao = new EstadoFinancieroYVentasSolicitudDAO();
            
            VentasTO[] ventas = dao.obtenerVentasSolicitud(numeroSolicitud, rut);
            
            if(ventas == null || ventas.length <= 0){
                if (this.getLogger().isEnabledFor(Level.DEBUG)) {
                    this.getLogger().debug("[validarVentas] Cliente no posee ventas para la solicitud");
                }
                resultadoSolicitud = agregaMensaje(rut, numeroSolicitud, "VALIDA_CLIENTE_SIN_VENTA", 
                    resultadoSolicitud, null);
            }
        }
        catch (Exception e) {
            if(this.getLogger().isEnabledFor(Level.WARN)){
                this.getLogger().warn(
                    "[validarVentas] Error al obtener ventas cliente: "
                        + ErroresUtil.extraeStackTrace(e));
            }
        }
        return resultadoSolicitud;
    }

    /**
     * Método encargado de realizar la validación de consistencia de ventas, para despliegue de alerta.
     * <p>
     * Registro de versiones:
     * <ul>
     * <li>1.0 28/05/2014 Gonzalo Bustamante V.(Sermaluc): Versión inicial.
     * </ul>
     * <p>
     * @param numeroSolicitud 
     *      numero de la solicitud
     * @param resultadoSolicitud
     *      Lista con mensajes de alertas para la solicitud de credito
     * @param rut
     *      Rut a consultar
     * @return lista de mensajes de alerta
     * @since 1.0
     */
    private RespuestaInvocacionesSolicitudTO[] validarInconsistenciasVentas(long rut,
        long numeroSolicitud, RespuestaInvocacionesSolicitudTO[] resultadoSolicitud) {
        
        if (this.getLogger().isEnabledFor(Level.INFO)) {
            this.getLogger().info("[validarInconsistenciasVentas] Iniciando metodo");
        }
        try {
            
            EstadoFinancieroYVentasSolicitudDAO dao = new EstadoFinancieroYVentasSolicitudDAO();
            
            if (this.getLogger().isEnabledFor(Level.DEBUG)) {
                this.getLogger().debug(
                    "[validarInconsistenciasVentas] Se obtienen todas las ventas de la solicitud");
            }
            VentasTO[] ventas = dao.obtenerVentasSolicitud(numeroSolicitud, rut);
            
            ArrayList ventasCompletas = new ArrayList(0);
            
            if(ventas != null){
                if (this.getLogger().isEnabledFor(Level.DEBUG)) {
                    this.getLogger().debug("[validarInconsistenciasVentas] Se busca el ultimo año completo");
                }

                boolean completo = true;
                
                for(int i = 0; i < ventas.length; i++){
                    completo = true;
                    HashMap ventasDelAnyo = ventas[i].getVentas();
                    if(ventasDelAnyo == null || ventasDelAnyo.isEmpty()){
                        completo = false;
                    }
                    else{
                        completo = true;
                        for(int j = 0; j < CANTIDAD_MESES_VENTA; j++){                        
                            double valorVenta = Double.parseDouble((ventasDelAnyo.get("mes" + j).toString()));
                            if(valorVenta == 0){
                                completo = false;
                                break;
                            }
                        }
                    }
                    if(completo == true){
                        ventasCompletas.add(ventas[i]);
                    }
                }
                if(ventasCompletas.size() > 0){
                    if (this.getLogger().isEnabledFor(Level.DEBUG)) {
                        this.getLogger()
                            .debug("[validarInconsistenciasVentas] Existen ventas con meses completos. "
                                + "Buscando estados financieros");
                    }
                    
                    EstadoFinancieroTO[] estadosFinancieros = dao.obtenerEstadoFinancieroSolicitud(
                        numeroSolicitud, rut);
                    boolean existeInconsistencia = false;
                    
                    if(estadosFinancieros != null && estadosFinancieros.length > 0){

                        if (this.getLogger().isEnabledFor(Level.DEBUG)) {
                            this.getLogger().debug("[validarInconsistenciasVentas] Se buscan estados financieros"
                                + " del mismo periodo que las ventas completas");
                        }
                        SimpleDateFormat formatoPeriodo = new SimpleDateFormat("yyyy");
                        
                        for(int k = 0; k < ventasCompletas.size(); k++){
                            EstadoFinancieroTO estadoFinanciero = null;
                            VentasTO venta = (VentasTO)ventasCompletas.get(k);
                            for(int j = 0; j < estadosFinancieros.length; j++){
                                if (venta.getAnyo() == Integer.parseInt(formatoPeriodo
                                    .format(estadosFinancieros[j].getPeriodoEstadoFinanciero()))) {
                                    estadoFinanciero = estadosFinancieros[j];
                                    break;
                                }
                            }
                            if(estadoFinanciero != null){
                                if (this.getLogger().isEnabledFor(Level.DEBUG)) {
                                    this.getLogger().debug(
                                        "[validarInconsistenciasVentas] Calculando variacion porcentual");
                                }
                                
                                BigDecimal sumatoriaVentas = new BigDecimal(venta.getTotalVentas());
                                BigDecimal ventasBalance = new BigDecimal(estadoFinanciero.getIngresos());
                                
                                BigDecimal resta = sumatoriaVentas.subtract(ventasBalance);
                                resta = resta.multiply(new BigDecimal(CIEN_PORCIENTO));
                                
                                BigDecimal variacionPorcentual = null;
                                
                                if(sumatoriaVentas.compareTo(ventasBalance) > 0){
                                    variacionPorcentual = resta.abs().divide(
                                    sumatoriaVentas, BigDecimal.ROUND_DOWN);
                                }
                                else{
                                    variacionPorcentual = resta.abs().divide(
                                    ventasBalance, BigDecimal.ROUND_DOWN);
                                }
                                
                                int limiteVariacion = Integer.parseInt(TablaValores.getValor(
                                    SolicitudUtil.TABLA_SOLICITUDES, "INCONSISTENCIA_VENTAS", "valor"));
                                
                                if (variacionPorcentual != null
                                    && variacionPorcentual
                                        .compareTo(new BigDecimal(limiteVariacion)) >= 0) {
                                    if (this.getLogger().isEnabledFor(Level.DEBUG)) {
                                        this.getLogger().debug(
                                            "[validarInconsistenciasVentas] [Variacion Porcentual = "
                                                + variacionPorcentual + "] del [año = "
                                                + venta.getAnyo()
                                                + " ] supera el limite, se añade mensaje");
                                    }
                                    existeInconsistencia = true;
                                }
                                else{
                                    if (this.getLogger().isEnabledFor(Level.DEBUG)) {
                                        this.getLogger().debug(
                                            "[validarInconsistenciasVentas] [Variacion Porcentual = "
                                                + variacionPorcentual + "] del [año = " + venta.getAnyo() 
                                                + " ] No existe inconsistencia, o no supera el limite");
                                    }
                                }
                            }
                            else{
                                if (this.getLogger().isEnabledFor(Level.DEBUG)) {
                                    this.getLogger().debug("[validarInconsistenciasVentas] No existen estados"
                                        + " financieros para el periodo");
                                }
                            }
                        }
                    }
                    else{
                        if (this.getLogger().isEnabledFor(Level.DEBUG)) {
                            this.getLogger().debug("[validarInconsistenciasVentas] No existen estados "
                                + " financieros para rut y solicitud");
                        }
                    }
                    if(existeInconsistencia){
                        resultadoSolicitud = agregaMensaje(rut, numeroSolicitud,
                            "VALIDA_INCONSISTENCIA_VENTAS", resultadoSolicitud, null);
                    }
                }
                else{
                    if (this.getLogger().isEnabledFor(Level.DEBUG)) {
                        this.getLogger()
                            .debug(
                                "[validarInconsistenciasVentas] No existe un año completo de ventas registrado");
                    }
                }
            }
        }
        catch (Exception e) {
            if(this.getLogger().isEnabledFor(Level.WARN)){
                this.getLogger().warn(
                    "[validarInconsistenciasVentas] Error al validar la inconsistencia de ventas: "
                        + ErroresUtil.extraeStackTrace(e));
            }
        }
        return resultadoSolicitud;
    }

    /**
     * Método encargado de realizar la validación de balances del cliente, para despliegue de alerta.
     * <p>
     * Registro de versiones:
     * <ul>
     * <li>1.0 28/05/2014 Gonzalo Bustamante V.(Sermaluc): Versión inicial.
     * </ul>
     * <p>
     * @param numeroSolicitud 
     *      numero de la solicitud
     * @param resultadoSolicitud
     *      Lista con mensajes de alertas para la solicitud de credito
     * @param rut
     *      Rut a consultar
     * @return lista de mensajes de alerta
     * @since 1.0
     */
    private RespuestaInvocacionesSolicitudTO[] validarBalancesCliente(long rut,
        long numeroSolicitud, RespuestaInvocacionesSolicitudTO[] resultadoSolicitud) {
        
        if (this.getLogger().isEnabledFor(Level.INFO)) {
            this.getLogger().info("[validarBalancesCliente] Iniciando metodo");
        }
        try {
            EstadoFinancieroYVentasSolicitudDAO dao = new EstadoFinancieroYVentasSolicitudDAO();
            EstadoFinancieroTO[] estadosFinancieros = dao.obtenerEstadoFinancieroSolicitud(
                numeroSolicitud, rut);
            
            if(estadosFinancieros == null || estadosFinancieros.length <= 0){
                if (this.getLogger().isEnabledFor(Level.DEBUG)) {
                    this.getLogger().debug(
                        "[validarBalancesCliente] No existen balances, se añade mensaje");
                }
                resultadoSolicitud = agregaMensaje(rut, numeroSolicitud,
                    "VALIDA_CLIENTE_SIN_BALANCE", resultadoSolicitud, null);
            }          
        }
        catch (Exception e) {
            if(this.getLogger().isEnabledFor(Level.ERROR)){
                this.getLogger().error(
                    "[validarBalancesCliente] Error al validar balances del cliente: "
                        + ErroresUtil.extraeStackTrace(e));
            }
        }
        return resultadoSolicitud;
    }
    
    /**
     * Método encargado de realizar la validación de vigencia de las relaciones del grupo economico.
     * <p>
     * Registro de versiones:
     * <ul>
     * <li>1.0 28/05/2014 Gonzalo Bustamante V.(Sermaluc): Versión inicial.
     * <li>1.1 18/03/2016 Desiree De Crescenzo (Sermaluc) - Patricio Valenzuela (Ing. Sw BCI): Se incorpora reglas de negocio
     * para indicar si la alerta es critica o no. 
     * <li>1.2 10/06/2016 Desiree De Crescenzo. (Sermaluc) - Patricio Valenzuela (Ing. Soft. BCI): Corrección para que valide aún y cuando se agrega un integrante de SUC.
     * </ul>
     * <p>
     * @param numeroSolicitud 
     *      numero de la solicitud
     * @param resultadoSolicitud
     *      Lista con mensajes de alertas para la solicitud de credito
     * @param rut
     *      Rut a consultar
     * @return lista de mensajes de alerta
     * @since 1.0
     */
    private RespuestaInvocacionesSolicitudTO[] validarVigenciaRelaciones(long rut, long numeroSolicitud,
        RespuestaInvocacionesSolicitudTO[] resultadoSolicitud) {

        if (this.getLogger().isEnabledFor(Level.INFO)) {
            this.getLogger().info(
                "[validarVigenciaRelaciones] Iniciando metodo para [rut = " + rut
                    + "][numeroSolicitud = " + numeroSolicitud + "]");
        }        
        try {
            boolean encontroAlerta = false;
            SolicitudesDAO dao = new SolicitudesDAO();
            IntegranteGrupoEconomicoTO[] integrantes = dao.obtenerIntegrantesGrupoEconomico(
                numeroSolicitud, rut);
            
            String codPersonaNatural = TablaValores.getValor(SolicitudUtil.TABLA_SOLICITUDES, "CATEGORIA_CLIENTE", "PERSONA_NATURAL");
            long rutLimite = Long.parseLong(TablaValores.getValor(SolicitudUtil.TABLA_SOLICITUDES,
                "VALIDA_RUT_FLUJO", "RUT_LIMITE"));
            
            String tipoCliente = "";
            DatosBasicosSolicitudDAO daoDatosBasicos = new DatosBasicosSolicitudDAO();
            DatosDelClienteVO datosCliente= null;
            datosCliente = daoDatosBasicos.obtenerDatosBasicosClienteEmpresa(numeroSolicitud, rut, RUTUtil.calculaDigitoVerificador(rut));
            
            if (datosCliente==null) {
                if (getLogger().isEnabledFor(Level.INFO)) {
                    getLogger().info("[validarVigenciaRelaciones] No existen datos básicos del cliente.");
                }
                resultadoSolicitud = this.agregaMensaje(rut, numeroSolicitud,
                    "VALIDA_VIGENCIA_RELACION_CRITICA", resultadoSolicitud, null);
            }
            else {
                tipoCliente = datosCliente.getRetornoTipCli().getDatosEmpresa().getTipoCliente();
                
                int contIntegrantesSinArt85 = 0;
                int contIntegrantesConArt85 = 0;
                if(integrantes == null || integrantes.length <= 0){
                    if (this.getLogger().isEnabledFor(Level.DEBUG)) {
                        this.getLogger().debug(
                            "[validarVigenciaRelaciones] el cliente no posee relaciones, se adjunta mensaje");
                    }
                    
                    encontroAlerta = true;    
                }
                else{
                    if (this.getLogger().isEnabledFor(Level.DEBUG)) {
                        this.getLogger().debug("[validarVigenciaRelaciones] el cliente posee relaciones,"
                            + " se valida vigencia de las fechas");
                    }
                    Date fechaAhora = new Date();
                    
                    boolean relacionsNoVigentes = false;
                    
                    for(int i = 0; i < integrantes.length; i ++) {
                        if (integrantes[i].getIndicadorArt85().equalsIgnoreCase(SolicitudUtil.ORIGEN_GRUPO_ART85)) { 
                            contIntegrantesConArt85 = contIntegrantesConArt85 + 1;
                            
                            Date fechaVigencia = integrantes[i].getFechaVer();
                            
                            if(fechaVigencia == null){
                                relacionsNoVigentes = true;
                                break;
                            }
                            else{
                                int maximoMesesVigencia = Integer.parseInt(TablaValores.getValor(SolicitudUtil
                                    .TABLA_SOLICITUDES,"ANTIGUEDAD_MAXIMA_EN_MESES", "valor"));
                                
                                if(maximoMesesVigencia < FechasUtil.diferenciaDeMeses(fechaAhora, fechaVigencia) ){
                                    relacionsNoVigentes = true;
                                    break;
                                }
                            }
                        }
                        else {
                            contIntegrantesSinArt85 = contIntegrantesSinArt85+1;
                        }
                    }
                                        
                    if(relacionsNoVigentes || (contIntegrantesSinArt85 > 0 && contIntegrantesConArt85 == 0)){
                        if (this.getLogger().isEnabledFor(Level.DEBUG)) {
                            this.getLogger().debug(
                                "[validarVigenciaRelaciones] el cliente posee relaciones no vigentes");
                        }
                        encontroAlerta = true;
                    }
                }
                if (encontroAlerta) {
                    if (rutLimite >= rut) {
                        if (getLogger().isEnabledFor(Level.INFO)) {
                            getLogger().info("[validarVigenciaRelaciones] Flujo rut menor a 50MM.");
                        }
                        if(tipoCliente.equalsIgnoreCase(codPersonaNatural)) {
                            if (getLogger().isEnabledFor(Level.INFO)) {
                                getLogger().info("[validarVigenciaRelaciones] El cliente rut " + rut + "es PN.");
                            }
                            resultadoSolicitud = this.agregaMensaje(rut, numeroSolicitud,
                                "VALIDA_VIGENCIA_RELACION_CRITICA", resultadoSolicitud, null); 
                        }
                        else {
                            resultadoSolicitud = this.agregaMensaje(rut, numeroSolicitud,
                                "VALIDA_VIGENCIA_RELACION", resultadoSolicitud, null);
                        }
                    }
                    else {
                        if (getLogger().isDebugEnabled()) {
                            getLogger().debug("[validarVigenciaRelaciones] Flujo rut mayor a 50MM.");
                        }
                        resultadoSolicitud = this.agregaMensaje(rut, numeroSolicitud,
                            "VALIDA_VIGENCIA_RELACION_CRITICA", resultadoSolicitud, null);
                    }  
                }
            }
        }
        catch (Exception e) {
            if(this.getLogger().isEnabledFor(Level.WARN)){
                this.getLogger().warn(
                    "[validarVigenciaRelaciones] Error al validar relaciones del [cliente = " +  rut + "]: "
                        + ErroresUtil.extraeStackTrace(e));
            }
        }
        return resultadoSolicitud;
    }
    
    /**
     * Método encargado de agregar un mensaje de alerta a una lista.
     * <p>
     * Registro de versiones:
     * <ul>
     * <li>1.0 28/05/2014 Gonzalo Bustamante V.(Sermaluc): Versión inicial.
     * </ul>
     * <p>
     * @param numeroSolicitud 
     *      numero de la solicitud
     * @param resultadoSolicitud
     *      Lista con mensajes de alertas para la solicitud de credito
     * @param rut
     *      Rut a consultar
     * @param clave
     *      Llave del mensaje a obtener desde tabla parametros
     * @param mensajeComplementario
     *      Mensaje complementario para alerta
     * @return lista de mensajes de alerta
     * @since 1.0
     */
    private RespuestaInvocacionesSolicitudTO[] agregaMensaje(long rut, long numeroSolicitud,
        String clave, RespuestaInvocacionesSolicitudTO[] resultadoSolicitud, String mensajeComplementario) {
        RespuestaInvocacionesSolicitudTO mensaje = new RespuestaInvocacionesSolicitudTO();

        if (this.getLogger().isEnabledFor(Level.INFO)) {
            this.getLogger().info("[agregaMensaje] Iniciando metodo");
        }

        mensaje.setRut(rut);
        mensaje.setDigitoVerificador(RUTUtil.calculaDigitoVerificador(rut));
        mensaje.setNumeroDeSolicitud(numeroSolicitud);
        mensaje.setCodigoMensaje(clave);
        mensaje.setCategoria(TablaValores.getValor(SolicitudUtil.TABLA_SOLICITUDES,
            clave, "CATEGORIA"));
        mensaje.setGlosaMensaje(TablaValores.getValor(SolicitudUtil.TABLA_SOLICITUDES,
            clave, "GLOSA_MENSAJE"));
        mensaje.setOrigen(TablaValores.getValor(SolicitudUtil.TABLA_SOLICITUDES, clave,
            "MODULO"));
        
        if(mensajeComplementario != null){
            mensaje.setGlosaMensaje(StringUtil.reemplazaCaracteres(mensaje.getGlosaMensaje(),
                CARACTER_COMODIN, mensajeComplementario));
        }

        if (resultadoSolicitud == null || resultadoSolicitud.length == 0) {
            if (this.getLogger().isEnabledFor(Level.DEBUG)) {
                this.getLogger().debug(
                    "[agregaMensaje] Sin mensajes anteriores, se crea nueva lista");
            }
            RespuestaInvocacionesSolicitudTO[] listaMensajes = new RespuestaInvocacionesSolicitudTO[1];
            listaMensajes[0] = mensaje;
            return listaMensajes;
        }
        else {
            if (this.getLogger().isEnabledFor(Level.DEBUG)) {
                this.getLogger().debug("[agregaMensaje] Añadiendo mensaje a lista existente");
            }
            ArrayList listado = new ArrayList(Arrays.asList(resultadoSolicitud));
            listado.add(mensaje);
            RespuestaInvocacionesSolicitudTO[] listaMensajes = new RespuestaInvocacionesSolicitudTO[listado
                .size()];
            listaMensajes = (RespuestaInvocacionesSolicitudTO[]) listado.toArray(listaMensajes);

            return listaMensajes;
        }
    }
    
    /**
     * Método encargado de transformar la lista de mensajes de alerta/errores de la validacion
     * de solicitud en un TO de justificacion para su correcto despliegue en la ventana de mensajes
     * de empresario.
     * <p>
     * Registro de versiones:
     * <ul>
     * <li>1.0 28/05/2014 Gonzalo Bustamante V.(Sermaluc): Versión inicial.
     * </ul>
     * <p>
     * @param listadoMensajes
     *      Lista con mensajes de alertas para la solicitud de credito
     * @return lista de mensajes de alerta en formato de presentacion
     * @since 1.0
     */
    private JustificacionSolicitudTO[] transformarTOJustificacion(
        RespuestaInvocacionesSolicitudTO[] listadoMensajes) {

        if (this.getLogger().isEnabledFor(Level.INFO)) {
            this.getLogger().info("[transformarTOJustificacion] Iniciando metodo");
        }
        
        if (listadoMensajes != null && listadoMensajes.length > 0) {
            
            if (this.getLogger().isEnabledFor(Level.DEBUG)) {
                this.getLogger().debug("[transformarTOJustificacion] Agrupando listado de mensajes por rut");
            }
            HashMap mapaRuts = new HashMap(0);

            for (int i = 0; i < listadoMensajes.length; i++) {
                RespuestaInvocacionesSolicitudTO mensaje = listadoMensajes[i];
                if(mapaRuts.get(Long.toString(mensaje.getRut())) == null){
                    ArrayList alertas = new ArrayList(0);
                    alertas.add(mensaje);
                    mapaRuts.put(Long.toString(mensaje.getRut()), alertas);
                }
                else{
                    ((ArrayList) mapaRuts.get(Long.toString(mensaje.getRut()))).add(mensaje);
                }
            }

            if (this.getLogger().isEnabledFor(Level.DEBUG)) {
                this.getLogger().debug("[transformarTOJustificacion] Despues de agrupar por rut");
            }
            
            Set set = mapaRuts.keySet();
            Object[] setLlaves = new Object[set.size()];
            setLlaves = set.toArray(setLlaves);

            ArrayList listaAlertas = new ArrayList(0);

            if (this.getLogger().isEnabledFor(Level.DEBUG)) {
                this.getLogger().debug("[transformarTOJustificacion] Transformando listado de mensajes");
            }
            
            for (int j = 0; j < setLlaves.length; j++) {
                JustificacionSolicitudTO mensajeAlerta = new JustificacionSolicitudTO();
                
                RespuestaInvocacionesSolicitudTO[] detalleAlertas = new RespuestaInvocacionesSolicitudTO[
                    ((ArrayList) mapaRuts.get((String) setLlaves[j])).size()];
                
                detalleAlertas = (RespuestaInvocacionesSolicitudTO[]) ((ArrayList) mapaRuts
                    .get((String) setLlaves[j])).toArray(detalleAlertas);
                
                mensajeAlerta.setDetalleAlertas(detalleAlertas);
                mensajeAlerta.setRut(detalleAlertas[0].getRut());
                mensajeAlerta.setDv(detalleAlertas[0].getDigitoVerificador());
                
                listaAlertas.add(mensajeAlerta);
            }
            
            if (this.getLogger().isEnabledFor(Level.DEBUG)) {
                this.getLogger().debug("[transformarTOJustificacion]Fin transformacion");
            }
            
            JustificacionSolicitudTO[] arregloAlertas = new JustificacionSolicitudTO[listaAlertas
                .size()];
            arregloAlertas = (JustificacionSolicitudTO[]) listaAlertas.toArray(arregloAlertas);

            return arregloAlertas;
        }
        else {
            if (this.getLogger().isEnabledFor(Level.DEBUG)) {
                this.getLogger().debug("[transformarTOJustificacion] No existen mensajes, se retorna nulo");
            }
            return null;
        }
    }

    /**
     * Método encargado de guardar las justificaciones a los mensajes de alerta 
     * generados de la validacion de solicitud generada en Banca Empresario.
     * <p>
     * Registro de versiones:
     * <ul>
     * <li>1.0 05/06/2014 Gonzalo Bustamante V.(Sermaluc): Versión inicial.
     * <li>1.1 22/04/2016 Desiree De Crescenzo R.(Sermaluc) - Patricio Valenzuela (Ing. Sw BCI): Ajustes de 
     *                    normativas relacionada a log.
     * </ul>
     * <p>
     * 
     * @param numeroSolicitud identificador de la solicitud.
     * @param justificacionesTO justificaciones a mensajes de validacion.
     * @throws GeneralException Excepcion general.
     * @since 1.0
     */
    public void guardarJustificaciones(long numeroSolicitud, JustificacionSolicitudTO[] justificacionesTO)
        throws GeneralException{
        if (this.getLogger().isEnabledFor(Level.INFO)) {
            this.getLogger().info(
                "[guardarJustificaciones] Iniciando metodo para [solicitud = " + numeroSolicitud
                    + "]");
        }
        try {
            SolicitudesDAO dao = new SolicitudesDAO();
     
            for(int i = 0; i < justificacionesTO.length; i++){
                dao.guardarJustificacionSolicitud(numeroSolicitud, justificacionesTO[i]);
            }
            if (this.getLogger().isEnabledFor(Level.INFO)) {
                this.getLogger().info("[guardarJustificaciones] Fin metodo");
            }
        }
        catch (Exception e) {
            if (this.getLogger().isEnabledFor(Level.ERROR)) {
                this.getLogger().error(
                    "[guardarJustificaciones] Error al registar las justificaciones: "
                        + ErroresUtil.extraeStackTrace(e));
            }
            throw new GeneralException(SolicitudCreditoException.ERROR_SERVICIOS,
                "Error al registrar las justificaciones");
        }
    }
    
    /**
     * Método encargado de obtener las justificaciones y alertas asociadas a una 
     * solicitud de la banca Emprendedor.
     * <p>
     * Registro de versiones:
     * <ul>
     * <li>1.0 05/06/2014 Mónica Garcés C.(Sermaluc): Versión inicial.
     * </ul>
     * <p>
     * 
     * @param numeroSolicitud identificador de la solicitud.
     * @return Arreglo de justificaciones y alertas.
     * @throws GeneralException excepcion general
     * @since 1.0
     */
    public JustificacionSolicitudTO[] obtenerAlertasSolicitud(long numeroSolicitud) throws GeneralException {

        if (this.getLogger().isEnabledFor(Level.INFO)) {
            this.getLogger().info(
                "[obtenerAlertasSolicitud] Inicio metodo: numeroSolicitud = " + numeroSolicitud);
        }
        
        JustificacionSolicitudTO[] justificacionAlertas = null;
        SolicitudesDAO solicitudesDAO = new SolicitudesDAO();

        if (this.getLogger().isEnabledFor(Level.DEBUG)) {
            this.getLogger().debug(
                "[obtenerDatosResolucionDeSolicitud] Antes de obtener las observaciones.");
        }

        justificacionAlertas = solicitudesDAO.obtenerAlertasSolicitud(numeroSolicitud);

        if (this.getLogger().isEnabledFor(Level.INFO)) {
            this.getLogger().info("[obtenerDatosResolucionDeSolicitud] Antes de retornar.");
        }
        return justificacionAlertas;
    }
    
    /**
     * 
     * Método encargado de validar la atribución del monto para los usuarios del área comercial al aporbar una solicitud.
     * <p>
     * Registro de versiones:<ul>
     * 
     * <li>1.0 29/12/2015 Desirée De Crescenzo R. (Sermaluc) - Patricio Valenzuela (ing. Soft. BCI): Versión inicial.
     * <li>1.1 27/04/2016 Desiree De Crescenzo. (Sermaluc) - Patricio Valenzuela (Ing. Sw BCI): Se ajusta
     * regla de negocio correspondiente a monto de deficit.
     * </ul><p>
     * @param rut Rut a evaluar.
     * @param numeroSolicitud Numero de la solicitud.
     * @param ejecutivo Ejecutivo solictante.
     * @param atribucionColaboradorTO Objeto con la informacion de atribuciones de garantia.
     * @param resultadoSolicitud Array que contiene los mensajes resultantes de las validaciones.
     * @return RespuestaInvocacionesSolicitudTO Arreglo con el mensaje resultante de las validaciones.   
     * @throws GeneralException En caso de ocurrir un error.
     * @throws SolicitudCreditoException En caso de ocurrir un error.
     * @since 1.8
     */
    private RespuestaInvocacionesSolicitudTO[] validarAtribucionGarantias(long rut, long numeroSolicitud, String ejecutivo, AtribucionColaboradorTO atribucionColaboradorTO, RespuestaInvocacionesSolicitudTO[] resultadoSolicitud)
        throws GeneralException, SolicitudCreditoException {
        
        SolicitudesDAO solicitudesDAO = new SolicitudesDAO();
        SolicitudTO solicitudTO = solicitudesDAO.obtenerSolicitud(numeroSolicitud);
        
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("[validarAtribucionGarantias] Antes de obtener la línea de crédito solicitada.");
            getLogger().debug("[validarAtribucionGarantias] Numero solicitud " +numeroSolicitud);
            getLogger().debug("[validarAtribucionGarantias] RUT " +rut);
        }
        LineaDeCreditoSolicitudDAO daoLinea = new LineaDeCreditoSolicitudDAO();
        LineaDeCreditoGlobalTO lineaSolicitada = daoLinea.consultarLineaYDetalleSolicitada(numeroSolicitud,
            solicitudTO.getRutTitular());
        
       if (lineaSolicitada != null) {
           if (lineaSolicitada.getMontoMaximo() > atribucionColaboradorTO.getMontoAtribucionConGtias() 
               && lineaSolicitada.getMontoMaximo() > atribucionColaboradorTO.getMontoAtribucionSinGtias()) {
               throw new GeneralException(SUC_ATRIBUCION_GARANTIA_MTOEXCEDIDO);
            }
            else if (lineaSolicitada.getTotalGarantia() == 0) {
                if (lineaSolicitada.getMontoMaximo() > atribucionColaboradorTO.getMontoAtribucionSinGtias()) {
                    throw new GeneralException(SUC_ATRIBUCION_GARANTIA_SINGARANTIA);
                } 
            }
            else {
                if (lineaSolicitada.getMontoMaximo() > atribucionColaboradorTO.getMontoAtribucionConGtias()) {
                    throw new GeneralException(SUC_ATRIBUCION_GARANTIA_CONGARANTIA); 
                }
                else 
                    if (lineaSolicitada.getDeficit() < 0) {
                        if (Math.abs(lineaSolicitada.getDeficit()) > atribucionColaboradorTO.getMontoAtribucionSinGtias()) {
                        throw new GeneralException(SUC_ATRIBUCION_GARANTIA_MTODEFICITMAYOR);
                    }
                }
            }  
       }
    
        return resultadoSolicitud;
    }
    
    /**
     * 
     * Método encargado de validar la antiguedad de una cuenta corriente.
     * <p>
     * Registro de versiones:<ul>
     * 
     * <li>1.0 29/12/2015 Desirée De Crescenzo R. (Sermaluc) - Patricio Valenzuela (ing. Soft. BCI): Versión inicial.
     * </ul><p>
     * @param rut Rut a evaluar.
     * @param numeroSolicitud Numero de la solicitud.
     * @param cuadroMando CuadroDeMandoTO[] Cuadro de mando de una solicitud.
     * @param antiguedadCuenta int con la cantidad de meses que debe tener la cuenta.
     * @param resultadoSolicitud Array que contiene los mensajes resultantes de las validaciones.
     * @return RespuestaInvocacionesSolicitudTO Arreglo con el mensaje resultante de las validaciones.   
     * @throws GeneralException En caso de ocurrir error.
     * @since 1.8
     */
    private RespuestaInvocacionesSolicitudTO[] validarCuentaCorriente(long rut, long numeroSolicitud, CuadroDeMandoTO[] cuadroMando, int antiguedadCuenta, RespuestaInvocacionesSolicitudTO[] resultadoSolicitud) throws GeneralException {
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("[validarCuentaCorriente][BCI_INI] Inicio de método.");
            getLogger().debug("[validarCuentaCorriente] con :" +antiguedadCuenta+ " meses de antiguedad.");
        } 
        int resultadoComparaFechas = 0;
        if (cuadroMando != null && cuadroMando.length > 0) {
            for (int i = 0; i < cuadroMando.length; i++) {
                Date fechaActual = new Date();
                if (cuadroMando[i].getFechaCuentaCorriente() != null) {
                    Date diferenciaFechas = FechasUtil.calculaDiaDeVencimiento(cuadroMando[i].getFechaCuentaCorriente(), antiguedadCuenta, 
                        AlertasSolicitudBO.INDICADOR_MES);
                    resultadoComparaFechas = FechasUtil.comparaDias(fechaActual, diferenciaFechas);
                }
                
                if (resultadoComparaFechas==-1 || cuadroMando[i].getFechaCuentaCorriente()==null) {
                    resultadoSolicitud = agregaMensaje(rut, numeroSolicitud, "VALIDA_ATRIBUCION_GARANTIA_CUENTACORRIENTE",
                        resultadoSolicitud, null);
                }
            }
        }
        
        if (this.getLogger().isEnabledFor(Level.DEBUG)) {
            this.getLogger().debug("[validarCuentaCorriente][BCI_FINOK] Fin del método");
        }
        return resultadoSolicitud;
    }
    
    /**
     * 
     * Método encargado de validar si un cliente posee marca de seguimiento.
     * <p>
     * Registro de versiones:<ul>
     * 
     * <li>1.0 29/12/2015 Desirée De Crescenzo R. (Sermaluc) - Patricio Valenzuela (ing. Soft. BCI): Versión inicial.
     * </ul><p>
     * @param rut Rut a evaluar.
     * @param numeroSolicitud Numero de la solicitud.
     * @param cuadroMando CuadroDeMandoTO[] Cuadro de mando de una solicitud.
     * @param marca String de marca de seguimiento a  validar.
     * @param resultadoSolicitud Array que contiene los mensajes resultantes de las validaciones.
     * @return RespuestaInvocacionesSolicitudTO Arreglo con el mensaje resultante de las validaciones.     
     * @throws GeneralException en caso de ocurrir error.
     * @since 1.8
     */
    private RespuestaInvocacionesSolicitudTO[] validarMarcaSeguimiento(long rut, long numeroSolicitud, CuadroDeMandoTO[] cuadroMando, String marca, RespuestaInvocacionesSolicitudTO[] resultadoSolicitud) throws GeneralException {
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("[validarMarcaSeguimiento][BCI_INI] Inicio de método.");
        }
            RiesgoDelegate riesgoDelegate;
            String clienteEnSSC="";

            if (cuadroMando != null && cuadroMando.length > 0) {
                for (int i = 0; i < cuadroMando.length; i++) {
                    long cliRut = cuadroMando[i].getRutSolicitante();
                    char cliDv = cuadroMando[i].getDigitoVerificadorSolicitante();
                    try {
                        riesgoDelegate = new RiesgoDelegate();
                        if (getLogger().isDebugEnabled()) {
                            getLogger().debug("[validarMarcaSeguimiento] rut :" +cliRut);
                            getLogger().debug("[validarMarcaSeguimiento] dv :"  +cliDv);
                            getLogger().debug("[validarMarcaSeguimiento] marca :" +marca);
                        } 
                        clienteEnSSC = riesgoDelegate.consultarEstadoClienteSSC(cliRut, cliDv);
                    }
                    catch (Exception e) {
                        if (getLogger().isEnabledFor(Level.ERROR)) {
                            getLogger().error("Error al obtener cliente en seguimiento de cartera", e);
                        }
                    }                
                   
                    if (getLogger().isDebugEnabled()) {
                        getLogger().debug("[validarMarcaSeguimiento] Marca de seguimiento:'" + clienteEnSSC + "'");
                    }
                    if (clienteEnSSC != null && !clienteEnSSC.trim().equals("")) {
                        if (clienteEnSSC.trim().equalsIgnoreCase(marca)) {
                            resultadoSolicitud = agregaMensaje(rut, numeroSolicitud, "VALIDA_ATRIBUCION_GARANTIA_MARCASEGUIMIENTO",
                                resultadoSolicitud, null);
                        }
                    }
                }
            }
        if (this.getLogger().isEnabledFor(Level.DEBUG)) {
            this.getLogger().debug("[validarMarcaSeguimiento][BCI_FINOK] Fin del método");
        }
        
        return resultadoSolicitud;
    }
    
    /**
     * 
     * Método encargado de validar la estrategia de riesgo de un cliente.
     * <p>
     * Registro de versiones:<ul>
     * 
     * <li>1.0 29/12/2015 Desirée De Crescenzo R. (Sermaluc) - Patricio Valenzuela (ing. Soft. BCI): Versión inicial.
     * <li>1.1 27/04/2016 Desirée De Crescenzo R. (Sermaluc) - Patricio Valenzuela (ing. Soft. BCI): Se agrega trim al comparar la estrategia
     * de riesgo. 
     * </ul><p>
     * 
     * @param rut Rut a evaluar.
     * @param numeroSolicitud Numero de la solicitud.
     * @param cuadroMando CuadroDeMandoTO[] Cuadro de mando de una solicitud.
     * @param perspectivas String de Perspectivas a restringir.
     * @param resultadoSolicitud Array que contiene los mensajes resultantes de las validaciones.
     * @return RespuestaInvocacionesSolicitudTO Arreglo con el mensaje resultante de las validaciones.
     * @throws GeneralException En caso de ocurrir un error.
     * @since 1.8
     */
    private RespuestaInvocacionesSolicitudTO[] validarPerspectiva(long rut, long numeroSolicitud, CuadroDeMandoTO[] cuadroMando, String perspectivas, RespuestaInvocacionesSolicitudTO[] resultadoSolicitud) throws GeneralException {
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("[validarPerspectiva][BCI_INI] Inicio de método.");
        }
        
        boolean clienteConEstratRiesgo = false;
        String[] estrategiasDeRiesgo = StringUtil.divide(perspectivas, ',');
        ServiciosSolicitudesDelegate delegate = new ServiciosSolicitudesDelegate();
        SolicitantePymeTO solicitantePymeTO = null;
        
        if (cuadroMando != null && cuadroMando.length > 0) {
            for (int i = 0; i < cuadroMando.length; i++) {
                clienteConEstratRiesgo = false;
                solicitantePymeTO = delegate.consultarPerspectivaCliente(cuadroMando[i].getRutSolicitante());
                if (getLogger().isDebugEnabled()) {
                    getLogger().debug("[validarPerspectiva]: Se obtubieron datos de perspectiva para: " + cuadroMando[i].getRutSolicitante());
                }
                if (solicitantePymeTO != null && solicitantePymeTO.getPerspectiva() != null) {
                        clienteConEstratRiesgo = StringUtil.estaContenidoEn(solicitantePymeTO.getPerspectiva().trim(), estrategiasDeRiesgo, true);
                        if (clienteConEstratRiesgo) {
                            resultadoSolicitud = agregaMensaje(rut, numeroSolicitud, "VALIDA_ATRIBUCION_GARANTIA_PERSPECTIVA",
                                resultadoSolicitud, null);
                        }
                }
            }
        }    
        
        if (getLogger().isEnabledFor(Level.DEBUG)) {
            getLogger().debug("[validarPerspectiva][BCI_FINOK] Fin del método");
        }
        
        return resultadoSolicitud;
    }
    
    /**
     * Método encargado de validar si un cliente posee los filtros de riesgos definidos
     * <p>
     * Registro de versiones:
     * <ul>
     * <li>1.0 12/01/2016 Desirée De Crescenzo R. (Sermaluc) - Patricio Valenzuela (Ing. Soft. BCI): Versión
     * inicial.
     * <li>1.1 07/07/2016 Gonzalo Paredes C.(TINet) - Jessica Ramirez (ing. Soft. BCI): Se agrega modificacion
     * para permitir la consulta de filtros de riesgo en linea
     * </ul>
     * <p>
     * 
     * @param rut Rut del solicitante.
     * @param numeroSolicitud Numero de Solicitud.
     * @param cuadroMando CuadroDeMandoTO[] Cuadro de mando de una solicitud.
     * @param filtros String con los filtros  a evaluar.
     * @param resultadoSolicitud Array que contiene los mensajes resultantes de las validaciones.
     * @return RespuestaInvocacionesSolicitudTO Arreglo con el mensaje resultante de las validaciones.
     * @throws GeneralException En caso de ocurrir un error.
     * @since 1.8
     */
    private RespuestaInvocacionesSolicitudTO[] validarFiltrosRiesgo(long rut, long numeroSolicitud, CuadroDeMandoTO[] cuadroMando, String filtros, RespuestaInvocacionesSolicitudTO[] resultadoSolicitud) throws GeneralException {
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("[validarFiltrosRiesgo][BCI_INI] Inicio de método.");
        }
        
        SolicitudesDAO solicitudesDAO = new SolicitudesDAO();
        
        String[] arregloFiltros = StringUtil.divide(filtros, ',');
        
        boolean presentaFiltro = false;
        boolean presentaRegistroFiltroRiesgo = false;
       
        if (cuadroMando != null && cuadroMando.length > 0) {
            for (int i = 0; i < cuadroMando.length; i++) {
                long cliRut = cuadroMando[i].getRutSolicitante();
                if (getLogger().isDebugEnabled()) {
                    getLogger().debug("[validarFiltrosRiesgo] rut :" + cliRut);

                } 
                FiltroRiesgoTO filtrosRiesgoTO = solicitudesDAO.consultarDatosFiltroRiesgo(cliRut);
                presentaFiltro = false;
                presentaRegistroFiltroRiesgo = false;
                
                if (filtrosRiesgoTO != null) {
                    String riesgosClientes = filtrosRiesgoTO.getValor();
                    riesgosClientes = StringUtil.reemplazaCaracteres(riesgosClientes, "[*", "");
                    riesgosClientes = StringUtil.reemplazaCaracteres(riesgosClientes, "*]", "");
                    
                    String[] arregloRiesgosClientes = StringUtil.divide(riesgosClientes, ";");
                    
                    if (arregloRiesgosClientes != null) {
                        for (int j = 0; j < arregloRiesgosClientes.length; j++) {
                            if (presentaFiltro) {
                                break;
                            }
                            for (int k = 0; k <  arregloFiltros.length; k++) {
                                if (arregloRiesgosClientes[j].indexOf(arregloFiltros[k]) != -1) {
                                    String[] divide = StringUtil.divide(arregloRiesgosClientes[j], "=");
                                    presentaRegistroFiltroRiesgo = true;
                                    if (divide[POSICION_COD_RIESGO].equalsIgnoreCase(arregloFiltros[k])) {
                                        if (divide[POSICION_VALOR_RIESGO].equalsIgnoreCase(FILTRO_RIESGO)) {
                                            resultadoSolicitud = agregaMensaje(cliRut, numeroSolicitud,
                                                "VALIDA_ATRIBUCION_GARANTIA_FILTRORIESGO", resultadoSolicitud,
                                                null);
                                            presentaFiltro = true;
                                            break;
                                        }
                                    }
                                }
                            } 
                        }

                        if (!presentaRegistroFiltroRiesgo) {
                            resultadoSolicitud = agregaMensaje(cliRut, numeroSolicitud,
                                "VALIDA_ATRIBUCION_SIN_REGISTRO_FILTRORIESGO", resultadoSolicitud, null);
                        }
                    }
                }
                else {
                    if (getLogger().isDebugEnabled()) {
                        getLogger().debug(
                            "[validarFiltrosRiesgo] No se encontró registro de filtro de riesgo para el cliente "
                                + cliRut);
                    }
                    resultadoSolicitud = agregaMensaje(cliRut, numeroSolicitud,
                        "VALIDA_ATRIBUCION_SIN_REGISTROBBDD_FILTRORIESGO", resultadoSolicitud, null);
                }

                if (!presentaFiltro) {
                    HashMap solicitante = new HashMap();
                    solicitante.put("rut", new Long(cliRut));

                    FlujoMotorDecisionPymeBO flujo = new FlujoMotorDecisionPymeBO();
                    FiltrosSolicitantePymeTO filtrosPyme = flujo
                        .obtenerFiltrosDeRiesgoSolicitante(solicitante);

                    if (filtrosPyme == null) {
                        resultadoSolicitud = agregaMensaje(cliRut, numeroSolicitud,
                            "VALIDA_ATRIBUCION_SIN_REGISTROBBDD_FILTRORIESGO", resultadoSolicitud, null);
                    }
                    else {
                        solicitante = (HashMap) flujo.setearValoresFiltroRiesgoSolicitantes(solicitante,
                            filtrosPyme);

                        String[] filtrosRiesgo = StringUtil.divide(TablaValores.getValor(
                            SolicitudUtil.TABLA_PYME, "EVAL_FILTROS_RIESGO_MTR_PYME_ALERTAS",
                            "LISTA_FILTROS"), ',');

                        if (filtrosRiesgo != null && filtrosRiesgo.length > 0) {
                            for (int j = 0; j < filtrosRiesgo.length; j++) {
                                if (filtrosRiesgo[j] != null && !filtrosRiesgo[j].trim().equals("")) {
                                    String valorFiltro = filtrosRiesgo[j];
                                    String valorAEvaluar = (String) solicitante.get(valorFiltro);
                                    if (valorAEvaluar.trim().equals(FILTRO_RIESGO)) {
                                        resultadoSolicitud = agregaMensaje(cliRut, numeroSolicitud,
                                            "VALIDA_ATRIBUCION_GARANTIA_FILTRORIESGO", resultadoSolicitud, null);
                                        break;
                                    }
                        }
                    }
                }
                else {
                    if (getLogger().isDebugEnabled()) {
                                getLogger().debug(
                                    "[validarFiltrosRiesgo] No se encontró llave en tabla de parametros");
                            }
                            resultadoSolicitud = agregaMensaje(cliRut, numeroSolicitud,
                                "VALIDA_ATRIBUCION_SIN_REGISTROBBDD_FILTRORIESGO", resultadoSolicitud,
                                null);
                        }
                    } 
                }
            }
        } 
        if (getLogger().isEnabledFor(Level.DEBUG)) {
            getLogger().debug("[validarFiltrosRiesgo][BCI_FINOK] Fin del método");
        }
        
        return resultadoSolicitud;
    }   
    
    /**
     * 
     * Método encargado de validar la calificacion que presenta el cliente.
     * <p>
     * Registro de versiones:<ul>
     * 
     * <li>1.0 12/01/2016 Desirée De Crescenzo R. (Sermaluc) - Patricio Valenzuela (Ing. Soft. BCI): Versión inicial.
     * </ul><p>
     * @param numeroSolicitud Numero de Solicitud.
     * @param cuadroMando CuadroDeMandoTO[] Cuadro de mando de una solicitud.
     * @param calificaciones String que contiene calificaciones a evaluar.
     * @param pi Double que representa el valor de pi a evaluar.
     * @param resultadoSolicitud Array que contiene los mensajes resultantes de las validaciones.
     * @return RespuestaInvocacionesSolicitudTO Arreglo con el mensaje resultante de las validaciones.
     * @throws GeneralException En caso de ocurrir un error.
     * @since 1.8
     */
    private RespuestaInvocacionesSolicitudTO[] validarCalificacion(long numeroSolicitud, CuadroDeMandoTO[] cuadroMando, String calificaciones, double pi, RespuestaInvocacionesSolicitudTO[] resultadoSolicitud) throws GeneralException {
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("[validarCalificacion][BCI_INI] Inicio de método.");
        }
        CalificacionesDAO calificacionesDAO = new CalificacionesDAO();
        
        String[] arregloCalificaciones = StringUtil.divide(calificaciones, ',');
        if (cuadroMando != null && cuadroMando.length > 0) {
            for (int i = 0; i < cuadroMando.length; i++) {
                long cliRut = cuadroMando[i].getRutSolicitante();
                boolean presentaCalificacion = false;
                boolean presentaProbabilidadIncumplimiento = false;
                
                if (getLogger().isDebugEnabled()) {
                    getLogger().debug("[validarCalificacion] rut :" +cliRut);

                } 
                String calificacionCliente = calificacionesDAO.obtenerCalificacionActual(cliRut);
                if (getLogger().isDebugEnabled()) {
                    getLogger().debug("[validarCalificacion] Calificacion:'" + calificacionCliente + "'");
                }
                if (calificacionCliente != null && !calificacionCliente.trim().equals("")) {
                    presentaCalificacion = StringUtil.estaContenidoEn(calificacionCliente.trim(), arregloCalificaciones, true);
                    presentaProbabilidadIncumplimiento = this.validarProbabilidadIncumplimiento(cliRut, pi);
                    
                    if (presentaCalificacion || presentaProbabilidadIncumplimiento) {
                        resultadoSolicitud = agregaMensaje(cliRut, numeroSolicitud, "VALIDA_ATRIBUCION_GARANTIA_PI_CALIFICACION",
                            resultadoSolicitud, null);
                    }
                }
            }
        } 
        
        if (getLogger().isEnabledFor(Level.DEBUG)) {
            getLogger().debug("[validarCalificacion][BCI_FINOK] Fin del método");
        }
        
        return resultadoSolicitud;
    }
    
    /**
     * 
     * Método encargado de validar la probabilidad de incumplimiento de un cliente.
     * <p>
     * Registro de versiones:<ul>
     * 
     * <li>1.0 12/01/2016 Desirée De Crescenzo R. (Sermaluc) - Patricio Valenzuela (Ing. Soft. BCI): Versión inicial.
     * </ul><p>
     * @param cliRutEvaluar Rut del cliente a evaluar.
     * @param pi double Valor de la probabilidad de incumplimiento a evaluar.
     * @return boolean <b>true</b> El cliente presenta probabilidad de incumplimiento; <b>false </b>en caso contrario.
     * @throws GeneralException En caso de ocurrir un error.
     * @since 1.8
     */
    private boolean validarProbabilidadIncumplimiento(long cliRutEvaluar, double pi) throws GeneralException {
        
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("[validarProbabilidadIncumplimiento][BCI_INI] Inicio de método.");
        }
        
        SolicitudesDAO solicitudesDAO = new SolicitudesDAO();    
        
        boolean presentaPI = false;
        double piMayor = 0;
        
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("[validarProbabilidadIncumplimiento] rut :" +cliRutEvaluar);
    
        } 
        ComportamientoClienteTO datosComportamiento = solicitudesDAO.consultarDatosDeComportamiento(cliRutEvaluar);
        ComportamientoClienteTO datosComportamientoIndividual = solicitudesDAO.consultarDatosComportamientoIndividual(cliRutEvaluar);
        ComportamientoClienteTO datosComportamientoGrupal = solicitudesDAO.consultarDatosComportamientoGrupal(cliRutEvaluar);
        
        if (datosComportamiento != null && datosComportamientoIndividual != null && datosComportamientoGrupal != null) {
            if (getLogger().isDebugEnabled()) {
                getLogger().debug("[validarProbabilidadIncumplimiento] Seteando datos de comportamiento");
            }
            double piConsumo = datosComportamiento.getProbabilidadIncumplimiento();
            double piIndividual = datosComportamientoIndividual.getProbabilidadIncumplimiento();
            double piGrupal = datosComportamientoGrupal.getProbabilidadIncumplimiento();
            
            if (piConsumo > piIndividual) {
                if (piConsumo > piGrupal) {
                    piMayor = piConsumo;
                }
                else {
                    piMayor = piGrupal;
                }
            }
            else if (piIndividual > piGrupal) {
                    piMayor = piIndividual;
                }
                else {
                    piMayor = piGrupal;
                }
            
            if (piMayor > pi) {
               presentaPI = true;
            }
        }
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("[validarProbabilidadIncumplimiento] PI Mayor:'" + piMayor + "'");
        }
        if (getLogger().isEnabledFor(Level.DEBUG)) {
            getLogger().debug("[validarProbabilidadIncumplimiento][BCI_FINOK] Fin del método");
        }
        
        return presentaPI;
    }
    
    /**
     * 
     * Método encargado de validar la atribución del monto para los usuarios del área comercial al aporbar una solicitud.
     * <p>
     * Registro de versiones:<ul>
     * 
     * <li>1.0 30/06/2017 Macarena Andrade. (Sermaluc) - Patricio Valenzuela (ing. Soft. BCI): Versión inicial.
     * </ul><p>
     * @param numeroSolicitud Numero de la solicitud.
     * @param ejecutivo Ejecutivo solictante.
     * @return respuesta respuesta de la validacion.  
     * @throws GeneralException En caso de ocurrir un error.
     * @throws SolicitudCreditoException En caso de ocurrir un error.
     * @since 1.10
     */
    public int validarAtribucionGarantias(long numeroSolicitud, String ejecutivo)
        throws GeneralException, SolicitudCreditoException {
        
        if (this.getLogger().isEnabledFor(Level.DEBUG)) {
            this.getLogger().debug("[validarAtribucionGarantias] ["+numeroSolicitud+"]Validación de atribuciones");
        }
        SolicitudesDAO solicitudesDAO = new SolicitudesDAO();
        AtribucionColaboradorTO atribucionColaboradorTO = solicitudesDAO.obtenerAtribucionesGarantiaUsuario(ejecutivo);
        if(atribucionColaboradorTO==null){
            return -1;
        }
        int respuesta= 1;
        

        SolicitudTO solicitudTO = solicitudesDAO.obtenerSolicitud(numeroSolicitud);
        
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("[validarAtribucionGarantias] Antes de obtener la línea de crédito solicitada.");
            getLogger().debug("[validarAtribucionGarantias] Numero solicitud " +numeroSolicitud);
        }
        LineaDeCreditoSolicitudDAO daoLinea = new LineaDeCreditoSolicitudDAO();
        LineaDeCreditoGlobalTO lineaSolicitada = daoLinea.consultarLineaYDetalleSolicitada(numeroSolicitud,
            solicitudTO.getRutTitular());
        
       if (lineaSolicitada != null) {
           if (lineaSolicitada.getMontoMaximo() > atribucionColaboradorTO.getMontoAtribucionConGtias() 
               && lineaSolicitada.getMontoMaximo() > atribucionColaboradorTO.getMontoAtribucionSinGtias()) {
               throw new GeneralException(SUC_ATRIBUCION_GARANTIA_MTOEXCEDIDO);
            }
            else if (lineaSolicitada.getTotalGarantia() == 0) {
                if (lineaSolicitada.getMontoMaximo() > atribucionColaboradorTO.getMontoAtribucionSinGtias()) {
                    throw new GeneralException(SUC_ATRIBUCION_GARANTIA_SINGARANTIA);
                } 
            }
            else {
                if (lineaSolicitada.getMontoMaximo() > atribucionColaboradorTO.getMontoAtribucionConGtias()) {
                    throw new GeneralException(SUC_ATRIBUCION_GARANTIA_CONGARANTIA); 
                }
                else 
                    if (lineaSolicitada.getDeficit() < 0) {
                        if (Math.abs(lineaSolicitada.getDeficit()) > atribucionColaboradorTO.getMontoAtribucionSinGtias()) {
                        throw new GeneralException(SUC_ATRIBUCION_GARANTIA_MTODEFICITMAYOR);
                    }
                }
            }  
       }
       if (getLogger().isEnabledFor(Level.INFO)) {
           getLogger().info("[validarAtribucionGarantias][BCI_FINOK]["+numeroSolicitud+"]"+ejecutivo+"]Fin del metodo.");
       }
        return respuesta;
    }
}
