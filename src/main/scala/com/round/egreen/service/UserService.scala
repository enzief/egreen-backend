// Copyright (c) 2018 Round, Inc.. All rights reserved.

package com.round.egreen.service

import java.util.UUID

import cats.data.EitherT
import cats.effect.Effect
import com.round.egreen.common.model.User
import com.round.egreen.cqrs.command
import com.round.egreen.repository.UserRepository
import io.circe.Json

class UserService[F[_]](eventService: EventService[F], repo: UserRepository[F]) {
  import UserRepository._
  import UserService._

  def checkUserExists(username: String)(implicit F: Effect[F]): F[Boolean] =
    repo.checkUserExists(username)

  def getUser(username: String)(implicit F: Effect[F]): EitherT[F, String, User] =
    repo.getUser(username)

  def createUser(cmd: command.CreateUser)(implicit F: Effect[F]): EitherT[F, String, Json] =
    for {
      user <- repo
               .getUser(cmd.username)
               .ensure(USER_EXISTS)(_ => false)
               .recover {
                 case USER_NOTFOUND =>
                   User(UUID.randomUUID(), cmd.username, cmd.encryptedPassword, cmd.roles)
               }
      json <- eventService.createUser(user)
    } yield json

}

object UserService {
  val USER_EXISTS = "user.exists"
}
