package fr.didictateur.inanutshell;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity
public class Folder implements Item {
	@PrimaryKey(autoGenerate = true)
	public long id;
	public String name;
	public Long parentId;

	public Folder() {}

	@Ignore
	public Folder(
			long id,
			String name,
			Long parentId
		) {
		this.id = id;
		this.name = name;
		this.parentId = parentId;
	}

	@Override
	public boolean isFolder() { return true; }

	@Override
	public String getTitle() { return name; }

	public long getId() { return id; }
	public Long getParentId() { return parentId; }

}
