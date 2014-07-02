
package adams.flow.transformer;

import adams.flow.core.InputConsumer;

public class TemplateTransformerNoCache
  extends TemplateTransformer {

  private static final long serialVersionUID = 5863512793682800042L;

  @Override
  protected String doExecute() {
    String result;

    result = setUpTemplate();

    if (result == null) {
      ((InputConsumer) m_Actor).input(m_InputToken);
      result = m_Actor.execute();
    }

    if (result != null) {
      System.out.println("WTF");
    }

    return result;
  }
}
