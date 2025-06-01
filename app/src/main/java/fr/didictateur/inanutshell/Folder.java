package fr.didictateur.inanutshell;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity
public class Folder implements Item {
	@PrimaryKey(autoGenerate = true)
	public int id;
	public String name;
	public Integer parentId;

	public Folder() {}

	@Ignore
	public Folder(
			int id,
			String name,
			Integer parentId
		) {
		this.id = id;
		this.name = name;
		this.parentId = parentId;
	}

	@Override
	public boolean isFolder() { return true; }

	@Override
	public String getTitle() { return name; }

	public int getId() { return id; }
	public Integer getParentId() { return parentId; }

}
