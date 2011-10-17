/*******************************************************************************
 * Este fichero forma parte del Cliente @firma.
 * El Cliente @firma es un aplicativo de libre distribucion cuyo codigo fuente puede ser consultado
 * y descargado desde http://forja-ctt.administracionelectronica.gob.es/
 * Copyright 2009,2010,2011 Gobierno de Espana
 * Este fichero se distribuye bajo  bajo licencia GPL version 2  segun las
 * condiciones que figuran en el fichero 'licence' que se acompana. Si se distribuyera este
 * fichero individualmente, deben incluirse aqui las condiciones expresadas alli.
 ******************************************************************************/

package es.gob.afirma.test.pades;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.security.KeyStore;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.cert.X509Certificate;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import junit.framework.Assert;

import org.junit.Test;

import es.gob.afirma.core.misc.AOUtil;
import es.gob.afirma.core.signers.AOSignConstants;
import es.gob.afirma.core.signers.AOSigner;
import es.gob.afirma.core.signers.AOSimpleSignInfo;
import es.gob.afirma.core.util.tree.AOTreeModel;
import es.gob.afirma.core.util.tree.AOTreeNode;
import es.gob.afirma.signers.pades.AOPDFSigner;
import es.gob.afirma.signers.pades.PAdESTimestamper;

/**
 * Pruebas del m&oacute;dulo PAdES de Afirma.
 * @author Tom&aacute;s Garc&iacute;a-Mer&aacute;s
 *
 */
public class TestPAdES {
    
    private static final String CERT_PATH = "ANF_PF_Activo.pfx"; //$NON-NLS-1$
    private static final String CERT_PASS = "12341234"; //$NON-NLS-1$
    private static final String CERT_ALIAS = "anf usuario activo"; //$NON-NLS-1$
    
    private static final Properties[] PADES_MODES;
    
    private static final String[] TEST_FILES = { "TEST_PDF.pdf" }; //$NON-NLS-1$

    private static final String TEST_FILE_PWD = "TEST_PDF_Password.pdf"; //$NON-NLS-1$
    private static final String TEST_FILE_CTF = "TEST_PDF_Certified.pdf"; //$NON-NLS-1$
    
    static {
        final Properties p1 = new Properties();
        p1.setProperty("format", AOSignConstants.SIGN_FORMAT_PDF); //$NON-NLS-1$
        p1.setProperty("mode", AOSignConstants.SIGN_MODE_IMPLICIT); //$NON-NLS-1$
        p1.setProperty("signReason", "test"); //$NON-NLS-1$ //$NON-NLS-2$
        p1.setProperty("signatureProductionCity", "madrid"); //$NON-NLS-1$ //$NON-NLS-2$
        p1.setProperty("signerContact", "sink@usa.net"); //$NON-NLS-1$ //$NON-NLS-2$
        p1.setProperty("policyQualifier", "http://google.com/"); //$NON-NLS-1$ //$NON-NLS-2$
        p1.setProperty("policyIdentifier", "2.16.724.1.3.1.1.2"); //$NON-NLS-1$ //$NON-NLS-2$
        p1.setProperty("policyIdentifierHash", "0"); //$NON-NLS-1$ //$NON-NLS-2$
        

        final Properties p2 = new Properties();
        p2.setProperty("format", AOSignConstants.SIGN_FORMAT_PDF); //$NON-NLS-1$
        p2.setProperty("mode", AOSignConstants.SIGN_MODE_EXPLICIT); //$NON-NLS-1$

        PADES_MODES = new Properties[] {
                p1, p2
        };
    }

    /** Algoritmos de firma a probar. */
    private final static String[] ALGOS = new String[] {
            AOSignConstants.SIGN_ALGORITHM_SHA1WITHRSA, 
            AOSignConstants.SIGN_ALGORITHM_SHA512WITHRSA,
            AOSignConstants.SIGN_ALGORITHM_SHA256WITHRSA,
            AOSignConstants.SIGN_ALGORITHM_SHA384WITHRSA,
    };
    
    /** Prueba de PDF con sello de tiempo contra la TSA de CATCert.
     * @throws Exception */
    @Test
    public void testTimestampedSignature() throws Exception {
        
        Logger.getLogger("es.gob.afirma").setLevel(Level.WARNING); //$NON-NLS-1$
        final PrivateKeyEntry pke;

        KeyStore ks = KeyStore.getInstance("PKCS12"); //$NON-NLS-1$
        ks.load(ClassLoader.getSystemResourceAsStream(CERT_PATH), CERT_PASS.toCharArray());
        pke = (PrivateKeyEntry) ks.getEntry(CERT_ALIAS, new KeyStore.PasswordProtection(CERT_PASS.toCharArray()));
        
        AOSigner signer = new AOPDFSigner();
        
        final byte[] testPdf = AOUtil.getDataFromInputStream(ClassLoader.getSystemResourceAsStream(TEST_FILES[0]));
        
        final String prueba = "Firma PAdES de PDF con sello de tiempo en SHA512withRSA"; //$NON-NLS-1$
        
        System.out.println(prueba);
        
        final Properties extraParams = new Properties();
        extraParams.put("tsaURL", PAdESTimestamper.CATCERT_TSP); //$NON-NLS-1$
        extraParams.put("tsaPolicy", PAdESTimestamper.CATCERT_POLICY); //$NON-NLS-1$
        extraParams.put("tsaRequireCert", PAdESTimestamper.CATCERT_REQUIRECERT); //$NON-NLS-1$
        extraParams.put("tsaHashAlgorithm", "SHA1"); //$NON-NLS-1$ //$NON-NLS-2$

        byte[] result = signer.sign(testPdf, "SHA512withRSA", pke, extraParams); //$NON-NLS-1$
        
        final File saveFile = File.createTempFile("TSA-", ".pdf"); //$NON-NLS-1$ //$NON-NLS-2$
        final OutputStream os = new FileOutputStream(saveFile);
        os.write(result);
        os.flush();
        os.close();
        System.out.println("Temporal para comprobacion manual: " + saveFile.getAbsolutePath()); //$NON-NLS-1$

        Assert.assertNotNull(prueba, result);
        Assert.assertTrue(signer.isSign(result));
        
    }
    
    
    /**
     * Prueba la firma de un PDF protegido con contrase&ntilde;a.
     * @throws Exception en cualquier error
     */
    @Test
    public void testPasswordSignature() throws Exception {
        Logger.getLogger("es.gob.afirma").setLevel(Level.WARNING); //$NON-NLS-1$
        final PrivateKeyEntry pke;

        KeyStore ks = KeyStore.getInstance("PKCS12"); //$NON-NLS-1$
        ks.load(ClassLoader.getSystemResourceAsStream(CERT_PATH), CERT_PASS.toCharArray());
        pke = (PrivateKeyEntry) ks.getEntry(CERT_ALIAS, new KeyStore.PasswordProtection(CERT_PASS.toCharArray()));
        
        AOSigner signer = new AOPDFSigner();
        
        final byte[] testPdf = AOUtil.getDataFromInputStream(ClassLoader.getSystemResourceAsStream(TEST_FILE_PWD));
         
        Assert.assertTrue("No se ha reconocido como un PDF", signer.isValidDataFile(testPdf)); //$NON-NLS-1$
    
        final String prueba = "Firma PAdES de PDF con contrasena en SHA512withRSA"; //$NON-NLS-1$
        
        System.out.println(prueba);
        
        final Properties extraParams = new Properties();
        extraParams.put("headLess", "true"); //$NON-NLS-1$ //$NON-NLS-2$
        extraParams.put("ownerPassword", "password"); //$NON-NLS-1$ //$NON-NLS-2$
        
        byte[] result = signer.sign(testPdf, "SHA512withRSA", pke, extraParams); //$NON-NLS-1$
        
        Assert.assertNotNull(prueba, result);
        Assert.assertTrue(signer.isSign(result));
        
    }
    
    
    /**
     * Prueba de firma convencional.
     * @throws Exception en cualquier error
     */
    @Test
    public void testSignature() throws Exception {
        
        Assert.assertEquals("file.signed.pdf", AOPDFSigner.getSignedName("file.pdf")); //$NON-NLS-1$ //$NON-NLS-2$
        
        Logger.getLogger("es.gob.afirma").setLevel(Level.WARNING); //$NON-NLS-1$
        final PrivateKeyEntry pke;
        final X509Certificate cert;

        KeyStore ks = KeyStore.getInstance("PKCS12"); //$NON-NLS-1$
        ks.load(ClassLoader.getSystemResourceAsStream(CERT_PATH), CERT_PASS.toCharArray());
        pke = (PrivateKeyEntry) ks.getEntry(CERT_ALIAS, new KeyStore.PasswordProtection(CERT_PASS.toCharArray()));
        cert = (X509Certificate) ks.getCertificate(CERT_ALIAS);
        
        AOSigner signer = new AOPDFSigner();
        
        String prueba;
        
        for (final Properties extraParams : PADES_MODES) {
            for (final String algo : ALGOS) {
                for (final String file : TEST_FILES) {
                    
                    final byte[] testPdf = AOUtil.getDataFromInputStream(ClassLoader.getSystemResourceAsStream(file)); 
                    
                    Assert.assertTrue("No se ha reconocido como un PDF", signer.isValidDataFile(testPdf)); //$NON-NLS-1$
                
                    prueba = "Firma PAdES en modo '" +  //$NON-NLS-1$
                    extraParams.getProperty("mode") +  //$NON-NLS-1$
                    "' con el algoritmo ': " + //$NON-NLS-1$
                    algo +
                    "' y el fichero '" +  //$NON-NLS-1$
                    file +
                    "'"; //$NON-NLS-1$
                    
                    System.out.println(prueba);
                    
                    byte[] result = signer.sign(testPdf, algo, pke, extraParams);
                    
                    Assert.assertNotNull(prueba, result);
                    Assert.assertTrue(signer.isSign(result));
                    
                    AOTreeModel tree = signer.getSignersStructure(result, false);
                    Assert.assertEquals("Datos", ((AOTreeNode) tree.getRoot()).getUserObject()); //$NON-NLS-1$
                    Assert.assertEquals("ANF Usuario Activo", ((AOTreeNode) tree.getRoot()).getChildAt(0).getUserObject()); //$NON-NLS-1$
                    
                    tree = signer.getSignersStructure(result, true);
                    Assert.assertEquals("Datos", ((AOTreeNode) tree.getRoot()).getUserObject()); //$NON-NLS-1$
                    AOSimpleSignInfo simpleSignInfo = (AOSimpleSignInfo) ((AOTreeNode) tree.getRoot()).getChildAt(0).getUserObject();
                    
                    //Assert.assertNotNull(simpleSignInfo.getSigningTime());
                    Assert.assertEquals(cert, simpleSignInfo.getCerts()[0]);    
                    
                    Assert.assertEquals("application/pdf", signer.getDataMimeType(result)); //$NON-NLS-1$
                    
                    Assert.assertEquals(result, signer.getData(result));
                    
                    Assert.assertEquals(AOSignConstants.SIGN_FORMAT_PDF, signer.getSignInfo(result).getFormat());
                    
                    final File saveFile = File.createTempFile(algo, ".pdf"); //$NON-NLS-1$
                    final OutputStream os = new FileOutputStream(saveFile);
                    os.write(result);
                    os.flush();
                    os.close();
                    System.out.println("Temporal para comprobacion manual: " + saveFile.getAbsolutePath()); //$NON-NLS-1$
                    
                }
                
                
            }
        }
    }
    

    /**
     * Prueba la firma de un PDF certificado.
     * @throws Exception en cualquier error
     */
    @Test
    public void testCertifiedSignature() throws Exception {
        Logger.getLogger("es.gob.afirma").setLevel(Level.WARNING); //$NON-NLS-1$
        final PrivateKeyEntry pke;

        KeyStore ks = KeyStore.getInstance("PKCS12"); //$NON-NLS-1$
        ks.load(ClassLoader.getSystemResourceAsStream(CERT_PATH), CERT_PASS.toCharArray());
        pke = (PrivateKeyEntry) ks.getEntry(CERT_ALIAS, new KeyStore.PasswordProtection(CERT_PASS.toCharArray()));
        
        AOSigner signer = new AOPDFSigner();
        
        final byte[] testPdf = AOUtil.getDataFromInputStream(ClassLoader.getSystemResourceAsStream(TEST_FILE_CTF));
         
        Assert.assertTrue("No se ha reconocido como un PDF", signer.isValidDataFile(testPdf)); //$NON-NLS-1$
    
        final String prueba = "Firma PAdES de PDF certificado en SHA512withRSA"; //$NON-NLS-1$
        
        System.out.println(prueba);
        
        Properties extraParams = new Properties();
        extraParams.put("allowSigningCertifiedPdfs", "true"); //$NON-NLS-1$ //$NON-NLS-2$
        
        byte[] result = signer.sign(testPdf, "SHA512withRSA", pke, extraParams); //$NON-NLS-1$
        
        Assert.assertNotNull(prueba, result);
        Assert.assertTrue(signer.isSign(result));
        
        extraParams = new Properties();
        extraParams.put("headLess", "true"); //$NON-NLS-1$ //$NON-NLS-2$
        
        boolean failed = false;
        try {
            result = signer.sign(testPdf, "SHA512withRSA", pke, extraParams); //$NON-NLS-1$
        }
        catch(final Exception e) {
            failed = true;
        }
        Assert.assertTrue("Deberia haber fallado", failed); //$NON-NLS-1$
        
        extraParams = new Properties();
        extraParams.put("allowSigningCertifiedPdfs", "false"); //$NON-NLS-1$ //$NON-NLS-2$
        
        failed = false;
        try {
            result = signer.sign(testPdf, "SHA512withRSA", pke, extraParams); //$NON-NLS-1$
        }
        catch(final Exception e) {
            failed = true;
        }
        Assert.assertTrue("Deberia haber fallado", failed); //$NON-NLS-1$
        
    }
}
