package networthcalculator.domain

object auth {

  sealed abstract class Role(val roleRepr: String) extends Product with Serializable

  object Role {
    case object Admin extends Role("Admin")
    case object User extends Role("User")
  }
}
