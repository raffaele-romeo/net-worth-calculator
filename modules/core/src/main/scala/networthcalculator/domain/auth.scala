package networthcalculator.domain

object auth {

  sealed abstract class Role(roleRepr: String)

  object Role {

    final case object Admin extends Role("Admin")
    final case object User extends Role("User")
  }
}
