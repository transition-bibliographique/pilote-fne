package fr.fne.batch.bnf.notice.datasources;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import fr.fne.batch.bnf.notice.model.Notice;

/**
 * 
 * @author Mickael KWINE KWOR MAN
 *
 */
public class DataSourceUtils {

	private static Logger logger = LoggerFactory.getLogger(DataSourceUtils.class);

	/**
	 * Constructeur qui ne doit etre jamais appele.
	 */
	private DataSourceUtils() {
		// Utilitaire, toutes les méthodes doivent etre statiques
	}

	/**
	 * Transformer un flux representant un fichier notice xml en String.
	 * 
	 * @param is
	 * 			{@link InputStream} Flux representant le fichier notice XML
	 * @return
	 * 			{@link String} La notice sous forme de String
	 * @throws IOException
	 */
	public static String inputStreamToString(InputStream is) throws IOException {
		StringBuilder sb = new StringBuilder();
		String line;
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		while ((line = br.readLine()) != null) {
			sb.append(line);
		}
		br.close();
		return sb.toString();
	}


	/**
	 * Transforme un string represetant un xml en une notice intermarc.
	 * 
	 * @param xml
	 * 		{@String} Xml
	 * @return
	 * 		{@link Notice}
	 */
	public static Notice parseXml(String xml) {
		Notice notice = null;
		try {			
			SAXParserFactory parserFactory = SAXParserFactory.newInstance();
			parserFactory.setNamespaceAware(true);
			SAXParser parser = parserFactory.newSAXParser();
			parser.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, ""); // Compliant
			parser.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, ""); // compliant
			
			XMLReader reader = parser.getXMLReader();
			NoticeXmlHandler handler = new NoticeXmlHandler();
			reader.setContentHandler(handler);
			reader.parse(new InputSource(new StringReader(xml)));
			notice = ((NoticeXmlHandler) reader.getContentHandler()).getNotice();
		} catch (SAXException e) {
			logger.error(String.format("Problème lors de la deserialization avec SAX : %s", e.getMessage()), e);
		} catch (IOException e) {
			logger.error(String.format("Problème lors de la lecture du xml : %s" , e.getMessage()), e);
		} catch (ParserConfigurationException e) {
			logger.error(String.format("Problème de configuration xml : %s" , e.getMessage()), e);			
		}
		return notice;
	}
}
