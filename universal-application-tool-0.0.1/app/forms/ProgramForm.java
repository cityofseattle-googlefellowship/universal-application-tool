package forms;

public class ProgramForm {
  private String name;
  private String description;

  public ProgramForm() {
    name = "";
    description = "";
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }
}
