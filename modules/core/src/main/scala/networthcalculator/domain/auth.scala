package networthcalculator.domain

object auth:
  enum Role:
    case Admin, User
  object Role:
    def fromString(s: String): Role = Role.valueOf(s)
