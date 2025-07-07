package gov.irs.formflow

object Log {
  def info(message: String): Unit = {
    System.err.println(s"[INFO] $message")
  }

  def warn(message: String): Unit = {
      System.err.println(s"[WARN] $message")
  }
}
