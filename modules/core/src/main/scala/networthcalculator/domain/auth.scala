package networthcalculator.domain

object auth {

  sealed abstract class Role(val roleRepr: String) extends Product with Serializable

  object Role {

    final case object Admin extends Role("Admin")
    final case object User extends Role("User")
  }
}
