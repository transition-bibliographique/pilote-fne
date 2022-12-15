package fr.fne.batch.bnf.notice.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class AlterN {

	private String ori;

	// On utilise une map pour réduire la recherche sur les zone
	// On utilise une list de zone dans la map pour traiter le cas de zone répétable
	private Map<String, List<ZoneN>> zoneNs;

	public AlterN() {
		this.zoneNs = new HashMap<>();
	}

	public void addZoneN(ZoneN zoneN) {
		if(this.getZoneNs().containsKey(zoneN.getTag())) {
			this.getZoneNs().get(zoneN.getTag()).add(zoneN);
		} else {
			List<ZoneN> listZoneNs = new ArrayList<>();
			listZoneNs.add(zoneN);
			this.getZoneNs().put(zoneN.getTag(), listZoneNs);
		}
	}

	public String getOri() {
		return ori;
	}

	public void setOri(String ori) {
		this.ori = ori;
	}

	public Map<String, List<ZoneN>> getZoneNs() {
		return zoneNs;
	}

	public void setZoneNs(Map<String, List<ZoneN>> zoneNs) {
		this.zoneNs = zoneNs;
	}

	@Override
	public int hashCode() {
		return Objects.hash(ori, zoneNs);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof AlterN)) {
			return false;
		}
		AlterN other = (AlterN) obj;
		return Objects.equals(ori, other.ori) && Objects.equals(zoneNs, other.zoneNs);
	}
}
