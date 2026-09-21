/*
#########################################################
################### INTEGRACIÓN FÁCIL ###################
+++++++++++++++++++++++++++++++++++++++++++++++++++++++++
# ESTE CÓDIGO FUNCIONA PARA LA VERSIÓN ONLINE Y OFFLINE
# Visita www.nubefact.com/integracion para mas información
+++++++++++++++++++++++++++++++++++++++++++++++++++++++++

#########################################################
#### FORMA DE TRABAJO ####
+++++++++++++++++++++++++++++++++++++++++++++++++++++++
# PASO 1: Conseguir una RUTA y un TOKEN para trabajar con NUBEFACT (Regístrate o ingresa a tu cuenta en www.nubefact.com).
# PASO 2: Generar un archivo en formato .JSON o .TXT con una estructura que se detalla en este documento.
# PASO 3: Enviar el archivo generado a nuestra WEB SERVICE ONLINE u OFFLINE según corresponda usando la RUTA y el TOKEN.
# PASO 4: Generamos el archivo XML y PDF (Según especificaciones de la SUNAT) y te devolveremos INSTANTÁNEAMENTE los datos del documento generado.
# Para ver el documento generado ingresa a www.nubefact.com/login con tus datos de acceso, y luego a la opción "Ver Facturas, Boletas y Notas"
# IMPORTANTE: Enviaremos el XML generado a la SUNAT y lo almacenaremos junto con el PDF, XML y CDR en la NUBE para que tu cliente pueda consultarlo en cualquier momento, si así lo desea.
+++++++++++++++++++++++++++++++++++++++++++++++++++++++
*/

package pruebafe;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.sql.Date;
import java.time.LocalDate;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;


public class ConsumingPost {
/*
#########################################################
#### PASO 1: CONSEGUIR LA RUTA Y TOKEN ####
+++++++++++++++++++++++++++++++++++++++++++++++++++++++
# - Regístrate gratis en www.nubefact.com/register
# - Ir la opción API (Integración).
# IMPORTANTE: Para que la opción API esté activada necesitas escribirnos a soporte@nubefact.com o llámanos al teléfono: 01 468 3535 (opción 2) o celular (WhatsApp) 955 598762.
+++++++++++++++++++++++++++++++++++++++++++++++++++++++
*/

//  RUTA para enviar documentos
    private String RUTA = "AQUI_VA_LA_RUTA";
    
//  TOKEN para enviar documentos    
    private String TOKEN = "AQUI_VA_EL_TOKEN";
    
    public void apiConsume(){     
        try {
/*
#########################################################
#### PASO 2: GENERAR EL ARCHIVO PARA ENVIAR A NUBEFACT ####
+++++++++++++++++++++++++++++++++++++++++++++++++++++++
# - MANUAL para archivo JSON en el link: https://goo.gl/WHMmSb
# - MANUAL para archivo TXT en el link: https://goo.gl/Lz7hAq
+++++++++++++++++++++++++++++++++++++++++++++++++++++++
*/            
            HttpClient cliente = new DefaultHttpClient();
            HttpPost post = new HttpPost(RUTA);
            post.addHeader("Authorization", "Token token=" + TOKEN); // Cabecera del token
            post.addHeader("Content-Type","application/json"); // Cabecera del Content-Type
                        
            
            JSONObject objetoCabecera = new JSONObject(); // Instancear el  segundario
            objetoCabecera.put("operacion","generar_comprobante");
            objetoCabecera.put("tipo_de_comprobante","1");
            objetoCabecera.put("serie","F001");
            objetoCabecera.put("numero","1");
            objetoCabecera.put("sunat_transaction","1");
            objetoCabecera.put("cliente_tipo_de_documento","6");
            objetoCabecera.put("cliente_numero_de_documento","20600695771");
            objetoCabecera.put("cliente_denominacion","NUBEFACT SA");
            objetoCabecera.put("cliente_direccion","CALLE LIBERTAD 116 MIRAFLORES - LIMA - PERU");
            objetoCabecera.put("cliente_email","tucliente@gmail.com");
            objetoCabecera.put("cliente_email_1","");
            objetoCabecera.put("cliente_email_2","");
            objetoCabecera.put("fecha_de_emision", "12-05-2017");
            objetoCabecera.put("fecha_de_vencimiento","");
            objetoCabecera.put("moneda","1");
            objetoCabecera.put("tipo_de_cambio","");
            objetoCabecera.put("porcentaje_de_igv","18.00");
            objetoCabecera.put("descuento_global","");
            objetoCabecera.put("total_descuento","");
            objetoCabecera.put("total_anticipo","");
            objetoCabecera.put("total_gravada","600");
            objetoCabecera.put("total_inafecta","");
            objetoCabecera.put("total_exonerada","");
            objetoCabecera.put("total_igv","108");
            objetoCabecera.put("total_gratuita","");
            objetoCabecera.put("total_otros_cargos","");
            objetoCabecera.put("total","708");
            objetoCabecera.put("percepcion_tipo","");
            objetoCabecera.put("percepcion_base_imponible","");
            objetoCabecera.put("total_percepcion","");
            objetoCabecera.put("total_incluido_percepcion","");
            objetoCabecera.put("detraccion", "false");
            objetoCabecera.put("observaciones", "");
            objetoCabecera.put("documento_que_se_modifica_tipo", "");
            objetoCabecera.put("documento_que_se_modifica_serie", "");
            objetoCabecera.put("documento_que_se_modifica_numero", "");
            objetoCabecera.put("tipo_de_nota_de_credito", "");
            objetoCabecera.put("tipo_de_nota_de_debito", "");
            objetoCabecera.put("enviar_automaticamente_a_la_sunat", "true");
            objetoCabecera.put("enviar_automaticamente_al_cliente", "false");
            objetoCabecera.put("codigo_unico", "");
            objetoCabecera.put("condiciones_de_pago", "");
            objetoCabecera.put("medio_de_pago", "");
            objetoCabecera.put("placa_vehiculo", "");
            objetoCabecera.put("orden_compra_servicio", "");
            objetoCabecera.put("tabla_personalizada_codigo", "");
            objetoCabecera.put("formato_de_pdf", "");
            
            JSONArray lista = new JSONArray();
            JSONObject detalle_linea_1 = new JSONObject();
            
            detalle_linea_1.put("unidad_de_medida", "NIU");
            detalle_linea_1.put("codigo", "001");
            detalle_linea_1.put("descripcion", "DETALLE DEL PRODUCTO");
            detalle_linea_1.put("cantidad", "1");
            detalle_linea_1.put("valor_unitario", "500");
            detalle_linea_1.put("precio_unitario", "590");
            detalle_linea_1.put("descuento", "");
            detalle_linea_1.put("subtotal", "500");
            detalle_linea_1.put("tipo_de_igv", "1");
            detalle_linea_1.put("igv", "90");
            detalle_linea_1.put("total", "590");
            detalle_linea_1.put("anticipo_regularizacion", "false");
            detalle_linea_1.put("anticipo_serie", "");
            detalle_linea_1.put("anticipo_documento_numero", "");
            
            JSONObject detalle_linea_2 = new JSONObject();
            
            detalle_linea_2.put("unidad_de_medida", "ZZ");
            detalle_linea_2.put("codigo", "001");
            detalle_linea_2.put("descripcion", "DETALLE DEL SERVICIO");
            detalle_linea_2.put("cantidad", "5");
            detalle_linea_2.put("valor_unitario", "20");
            detalle_linea_2.put("precio_unitario", "23.60");
            detalle_linea_2.put("descuento", "");
            detalle_linea_2.put("subtotal", "100");
            detalle_linea_2.put("tipo_de_igv", "1");
            detalle_linea_2.put("igv", "18");
            detalle_linea_2.put("total", "118");
            detalle_linea_2.put("anticipo_regularizacion", "false");
            detalle_linea_2.put("anticipo_serie", "");
            detalle_linea_2.put("anticipo_documento_numero", "");
            
            lista.add(detalle_linea_1);
            lista.add(detalle_linea_2);
            
            objetoCabecera.put("items", lista);
/*
#########################################################
#### PASO 3: ENVIAR EL ARCHIVO A NUBEFACT ####
+++++++++++++++++++++++++++++++++++++++++++++++++++++++
# SI ESTÁS TRABAJANDO CON ARCHIVO JSON
# - Debes enviar en el HEADER de tu solicitud la siguiente lo siguiente:
# Authorization = Token token="8d19d8c7c1f6402687720eab85cd57a54f5a7a3fa163476bbcf381ee2b5e0c69"
# Content-Type = application/json
# - Adjuntar en el CUERPO o BODY el archivo JSON o TXT
# SI ESTÁS TRABAJANDO CON ARCHIVO TXT
# - Debes enviar en el HEADER de tu solicitud la siguiente lo siguiente:
# Authorization = Token token="8d19d8c7c1f6402687720eab85cd57a54f5a7a3fa163476bbcf381ee2b5e0c69"
# Content-Type = text/plain
# - Adjuntar en el CUERPO o BODY el archivo JSON o TXT
+++++++++++++++++++++++++++++++++++++++++++++++++++++++
*/

            StringEntity parametros = new StringEntity(objetoCabecera.toString() ,"UTF-8");
            post.setEntity(parametros);
            HttpResponse response = cliente.execute(post);     
            BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));           
            String linea = "";
            if((linea = rd.readLine()) != null) {
                
                JSONParser parsearRsptaJson = new JSONParser();
                JSONObject json_rspta = (JSONObject) parsearRsptaJson.parse(linea);
 
/*
#########################################################
#### PASO 4: LEER RESPUESTA DE NUBEFACT ####
+++++++++++++++++++++++++++++++++++++++++++++++++++++++
# Recibirás una respuesta de NUBEFACT inmediatamente lo cual se debe leer, verificando que no haya errores.
# Debes guardar en la base de datos la respuesta que te devolveremos.
# Escríbenos a soporte@nubefact.com o llámanos al teléfono: 01 468 3535 (opción 2) o celular (WhatsApp) 955 598762
# Puedes imprimir el PDF que nosotros generamos como también generar tu propia representación impresa previa coordinación con nosotros.
# La impresión del documento seguirá haciéndose desde tu sistema. Enviaremos el documento por email a tu cliente si así lo indicas en el archivo JSON o TXT.
+++++++++++++++++++++++++++++++++++++++++++++++++++++++               
*/                
                if(json_rspta.get("errors") != null ){
                    System.out.println("Error => " + json_rspta.get("errors"));
                }else{
                    
                    
                    JSONParser parsearRsptaDetalleOK = new JSONParser();
                    JSONObject json_rspta_ok = (JSONObject) parsearRsptaDetalleOK.parse(json_rspta.get("invoice").toString());
                    
                    System.out.println(json_rspta_ok.get("tipo_de_comprobante"));
                    System.out.println(json_rspta_ok.get("serie"));
                    System.out.println(json_rspta_ok.get("numero"));
                    System.out.println(json_rspta_ok.get("enlace"));
                    System.out.println(json_rspta_ok.get("aceptada_por_sunat"));
                    System.out.println(json_rspta_ok.get("sunat_description"));
                    System.out.println(json_rspta_ok.get("sunat_note"));
                    System.out.println(json_rspta_ok.get("sunat_responsecode"));
                    System.out.println(json_rspta_ok.get("sunat_soap_error"));
                    System.out.println(json_rspta_ok.get("pdf_zip_base64"));
                    System.out.println(json_rspta_ok.get("xml_zip_base64"));
                    System.out.println(json_rspta_ok.get("cdr_zip_base64"));
                    System.out.println(json_rspta_ok.get("cadena_para_codigo_qr"));
                    System.out.println(json_rspta_ok.get("codigo_hash"));
                } 
            }   
        } catch (UnsupportedEncodingException ex1) {
            System.err.println("Error UnsupportedEncodingException: "+ex1.getMessage());
        } catch (IOException ex2) {
            System.err.println("Error IOException: "+ex2.getMessage());
        } catch (Exception ex3){
            System.err.println("Exepction: "+ex3.getMessage());
        }
    }
}
