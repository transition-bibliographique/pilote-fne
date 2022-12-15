package fr.fne.batch.bnf.notice.datasources;

import org.apache.logging.log4j.util.Strings;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import fr.fne.batch.bnf.notice.model.AlterN;
import fr.fne.batch.bnf.notice.model.AttrN;
import fr.fne.batch.bnf.notice.model.Notice;
import fr.fne.batch.bnf.notice.model.PexN;
import fr.fne.batch.bnf.notice.model.PosN;
import fr.fne.batch.bnf.notice.model.SousZoneN;
import fr.fne.batch.bnf.notice.model.ZoneN;

/**
 * Parseur SAX2 qui lit du XML InterXMarc. Le parseurs peut fonctionner avec deux types de sources :
 * Le filesystem (puit) et le webservice (noticeservice).
 * 
 * @author Thibaud SENALADA
 * 
 * @author Mickael KWINE KWOR MAN
 * 
 */
public class NoticeXmlHandler extends DefaultHandler {	

	private boolean firstNotice = true; // Distinction notices analytiques
	private Notice notice;

	private Notice noticeNiv1;
	private boolean bNoticeNiv1 = false;

	private Notice noticeNiv2;
	private boolean bNoticeNiv2 = false;

	private ZoneN zoneN;
	private boolean bZoneN = false;

	private PosN posN;
	private boolean bPosN = false;

	private SousZoneN sousZoneN;
	private boolean bSousZoneN = false;

	private PexN pexN;
	private boolean bPexN = false;

	private AttrN attrN;
	private boolean bAttrN = false;

	private AlterN alterN;
	private boolean bAlterN = false;

	private boolean bSubFieldValueFirst = false;

	private StringBuilder stringBuilder;

	//début du parsing
	@Override
	public void startDocument() throws SAXException {
		// nothing
	}

	//fin du parsing
	@Override
	public void endDocument() throws SAXException {
		// nothing
	}

	@Override
	public void startElement(String nameSpace, String localName, String qName, Attributes attr) throws SAXException {

		this.stringBuilder = new StringBuilder();

		String numero = "Numero";

		if ("record".equals(localName)) {
			if (this.firstNotice) { // C'est la premiere notice que je croise
				this.notice = new Notice();
				this.notice.setNumero(attr.getValue(numero));
				this.notice.setFormat(attr.getValue("format"));
				this.notice.setType(attr.getValue("type"));
				this.firstNotice = false;
			} else { // Ce n'est pas la premiere fois que je croise une notice donc c'est une analytique
				// Test si le record est un record de niveau 1
				if ("1".equals(attr.getValue("Niv"))) {
					this.noticeNiv1 = new Notice();
					this.noticeNiv1.setNumero(attr.getValue(numero));
					this.bNoticeNiv1 = true;
				}
				// Test si le record est un record de niveau 2
				if ("2".equals(attr.getValue("Niv"))) {
					this.noticeNiv2 = new Notice();
					this.noticeNiv2.setNumero(attr.getValue(numero));
					this.bNoticeNiv2 = true;
				}
			}
			this.attrN = null;
			this.bAttrN = false;
		}

		// On test si le tag correspond à l'équivalent d'une zone, balise <leader>, <controlfield> ou <datafield>
		else if ("leader".equals(localName) || "controlfield".equals(localName) || "datafield".equals(localName)) {
			this.zoneN = new ZoneN();
			this.zoneN.setTag(attr.getValue("tag"));
			this.zoneN.setInd1(attr.getValue("ind1"));
			this.zoneN.setInd2(attr.getValue("ind2"));
			this.bZoneN = true;
		}

		// Si on est dans une balise <Pos>
		else if ("Pos".equals(localName)) {
			this.posN = new PosN(attr.getValue("Code"), attr.getValue("Sens"));
			this.bPosN = true;
		}

		// Si on est dans une balise <subfield>
		else if ("subfield".equals(localName)) {
			this.sousZoneN = new SousZoneN(attr.getValue("code"), attr.getValue("Barre"), attr.getValue("Sens"));
			this.sousZoneN.setNumero(this.notice.getNumero());
			this.bSousZoneN = true;
			this.bSubFieldValueFirst = false;
		}

		// Si on est dans une balise <PEX>
		else if ("PEX".equals(localName)) {
			this.pexN = new PexN();
			this.pexN.setNr(attr.getValue("NR"));
			this.pexN.setNo(attr.getValue("NO"));
			this.bPexN = true;
		}

		// Si on est dans une balise <Attr>
		else if ("Attr".equals(localName)) {
			this.attrN = new AttrN();
			this.attrN.setNom(attr.getValue("Nom"));
			this.attrN.setRang(attr.getValue("Rang"));
			this.attrN.setRep(attr.getValue("Rep"));
			this.attrN.setSens(attr.getValue("Sens"));
			this.bAttrN = true;
		}

		// Si on est dans une balise <Alter>
		else if ("Alter".equals(localName)) {
			this.alterN = new AlterN();
			this.alterN.setOri(attr.getValue("Ori"));
			this.bAlterN = true;
		}
	}

	@Override
	public void endElement(String nameSpace, String localName, String qName) throws SAXException {

		// Si on est dans une balise record
		if ("record".equals(localName)) {
			//Si on est pas dans le premier record
			if (!this.firstNotice) {
				// Si on est à la fin de la balise record niv 1
				if (this.bNoticeNiv1 && !this.bNoticeNiv2) {
					this.notice.getNotices().add(this.noticeNiv1);
					this.bNoticeNiv1 = false;
					this.noticeNiv1 = null;
				}

				// Si on est à la fin de la balise record niv 2
				if (this.bNoticeNiv2) {
					this.noticeNiv1.getNotices().add(this.noticeNiv2);
					this.bNoticeNiv2 = false;
					this.noticeNiv2 = null;
				}
			}
		}

		// Si on est dans un <Alter>, et que l'on est pas dans une zone
		else if (this.bAlterN && !this.bZoneN) {
			// Si on est dans le cas ou on insert dans la notice de niveau 2 en cours de traitement
			if (this.noticeNiv2 != null) {
				this.noticeNiv2.getAlterNs().add(this.alterN);
			}
			// Si on est dans le cas ou on insert dans la notice de niveau 1 en cours de traitement
			else if (this.noticeNiv1 != null) {
				this.noticeNiv1.getAlterNs().add(this.alterN);
				// Si on est dans le cas ou on insert dans la notice parent
			} else {
				this.notice.getAlterNs().add(this.alterN);
			}
			this.alterN = null;
			this.bAlterN = false;
		}

		// Si on est dans une zone (<leader>, <controlfield>, <datafield>)
		else if (this.bZoneN && !this.bPosN && !this.bSousZoneN) {
			// Dans le cas ou l'on doit insérer dans un <alter>
			// (Sachant que seulement les datafields sont attendu pour le cas des <alter>)
			if (this.alterN != null) {
				this.alterN.addZoneN(this.zoneN);
			}
			// Si on est dans le cas ou on insert dans la notice de niveau 2 en cours de traitement
			else if (this.noticeNiv2 != null) {
				this.noticeNiv2.addZoneN(this.zoneN);
			}
			// Si on est dans le cas ou on insert dans la notice de niveau 1 en cours de traitement
			else if (this.noticeNiv1 != null) {
				this.noticeNiv1.addZoneN(this.zoneN);
				// Si on est dans le cas ou on insert dans la notice parent
			} else {
				this.notice.addZoneN(this.zoneN);
			}
			this.zoneN = null;
			this.bZoneN = false;
		}

		// Si on est dans un <subfield>, et que l'on n'est pas dans un <pos>
		else if (this.bSousZoneN && !this.bPosN) {

			if (this.sousZoneN.getValue() == null) {
				this.sousZoneN.setValue(Strings.EMPTY);
			}

			this.zoneN.addSousElementN(this.sousZoneN);
			this.sousZoneN = null;
			this.bSousZoneN = false;
		}

		// Si on est dans un <pos>
		else if (this.bPosN) {
			this.bSubFieldValueFirst = true;
			// Dans le cas d'un <pos> dans un <subfield>
			if (this.bSousZoneN) {
				this.sousZoneN.getPosNs().add(this.posN);
				// Dans le cas <pos> dans une zone
			} else {
				this.zoneN.addSousElementN(this.posN);
			}
			this.posN = null;
			this.bPosN = false;
		}

		// Si l'on est dan un <pex>, et que l'on n'est pas dans un <Attr>
		else if (this.bPexN && !this.bAttrN) {
			// Si on est dans le cas ou on insert dans la notice de niveau 2 en cours de traitement
			if (this.noticeNiv2 != null) {
				this.noticeNiv2.getPexNs().add(this.pexN);
			}
			// Si on est dans le cas ou on insert dans la notice de niveau 1 en cours de traitement
			else if (this.noticeNiv1 != null) {
				this.noticeNiv1.getPexNs().add(this.pexN);
				// Si on est dans le cas ou on insert dans la notice parent
			} else {
				this.notice.getPexNs().add(this.pexN);
			}
			this.pexN = null;
			this.bPexN = false;
		}

		// Si on est dans un <Attr>
		else if (this.bAttrN) {
			// Si on doit l'ajouter dans une pex
			if (this.bPexN) {
				this.pexN.getAttrNs().add(this.attrN);
				// Sinon on l'ajoute dans un record
			} else {
				// Si on est dans le cas ou on insert dans la notice de niveau 2 en cours de traitement
				if (this.noticeNiv2 != null) {
					this.noticeNiv2.getAttrNs().add(this.attrN);
				}
				// Si on est dans le cas ou on insert dans la notice de niveau 1 en cours de traitement
				else if (this.noticeNiv1 != null) {
					this.noticeNiv1.getAttrNs().add(this.attrN);
					// Si on est dans le cas ou on insert dans la notice parent
				} else {
					this.notice.getAttrNs().add(this.attrN);
				}
			}
			this.attrN = null;
			this.bAttrN = false;
		}
	}

	@Override
	// Cette methode ce lance après chaque endElement
	public void characters(char[] caracteres, int debut, int longueur) throws SAXException {

		this.stringBuilder.append(new String(caracteres, debut, longueur));
		String donnees = this.stringBuilder.toString();

		// On ajoute la valeur de <pos>
		if (this.bPosN) {
			this.posN.setValue(donnees.replace("\n", "").replace("\r", "").replace("\t", "").replaceAll("\\s+$", "#"));
		}

		// On ajoute la valeur de <subfield>
		if (this.bSousZoneN && !this.bPosN && !this.bSubFieldValueFirst) {
			this.sousZoneN.setValue(donnees);
		}

		// On ajoute la valeur de <Attr>
		if (this.bAttrN) {
			this.attrN.setValue(donnees);
		}
	}

	public Notice getNotice() {
		return this.notice;
	}
}