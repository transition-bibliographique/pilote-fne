package fr.fne.batch.bnf.notice.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PexN {
	
	private String nr;
	
	private String no;
	
	private List<AttrN> attrNs;

	public PexN() {
		this.attrNs = new ArrayList<>();
	}
	
	public String getNr() {
		return nr;
	}

	public void setNr(String nr) {
		this.nr = nr;
	}

	public String getNo() {
		return no;
	}

	public void setNo(String no) {
		this.no = no;
	}

	public List<AttrN> getAttrNs() {
		return attrNs;
	}

	public void setAttrNs(List<AttrN> attrNs) {
		this.attrNs = attrNs;
	}

	@Override
	public int hashCode() {
		return Objects.hash(attrNs, no, nr);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof PexN)) {
			return false;
		}
		PexN other = (PexN) obj;
		return Objects.equals(attrNs, other.attrNs) && Objects.equals(no, other.no) && Objects.equals(nr, other.nr);
	}
}
