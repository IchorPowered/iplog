/*
 * This file is part of IPLog, licensed under the MIT License.
 *
 * Copyright (c) 2017 Meronat <http://meronat.com>
 * Copyright (c) Contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.meronat.iplog.commands;

import com.meronat.iplog.IPLog;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.service.pagination.PaginationService;
import org.spongepowered.api.service.user.UserStorageService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class HistoryCommand implements CommandExecutor {

    @Override
    public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
        final Optional<User> optionalUser = args.getOne("player");
        final Optional<InetAddress> optionalAddress = args.getOne("ip");

        if (optionalUser.isPresent() && optionalAddress.isPresent()) {
            throw new CommandException(Text.of(TextColors.RED, "You must specify either an IP address or player, but not both."));
        }

        final DateTimeFormatter timeFormatter = DateTimeFormatter.ISO_DATE_TIME.withLocale(src.getLocale());

        if (optionalAddress.isPresent()) {
            IPLog.getPlugin().newChain()
                    .asyncFirst(() -> {
                        final Map<UUID, LocalDateTime> users = IPLog.getPlugin().getStorage().getPlayersAndTime(optionalAddress.get());

                        if(users.isEmpty()) {
                            src.sendMessage(Text.of(TextColors.RED, "There are no players associated with this IP address."));
                            return null;
                        }
                        return users;
                    })
                    .abortIfNull()
                    .syncLast(users -> {
                        final UserStorageService userStorageService = Sponge.getServiceManager().provide(UserStorageService.class).get();
                        final List<Text> contents = new ArrayList<>();

                        users.forEach((key, value) -> userStorageService.get(key).ifPresent(user ->
                                contents.add(Text.of(TextColors.DARK_GREEN, user.getName(), TextColors.GRAY, "    ", timeFormatter.format(value)))));

                        Sponge.getServiceManager().provide(PaginationService.class).get().builder()
                                .title(Text.of(TextColors.DARK_GREEN, "User History Associated With", TextColors.GREEN, optionalAddress.get().toString()))
                                .contents(contents)
                                .linesPerPage(14)
                                .padding(Text.of(TextColors.GRAY, "="))
                                .sendTo(src);
                    }).execute();
        } else if (optionalUser.isPresent()) {
            IPLog.getPlugin().newChain()
                    .asyncFirst(() -> {
                        final Map<String, LocalDateTime> addresses = IPLog.getPlugin().getStorage().getAddressesAndTime(optionalUser.get().getUniqueId());

                        if(addresses.isEmpty()) {
                            src.sendMessage(Text.of(TextColors.RED, "There are no IP addresses associated with this user."));
                            return null;
                        }

                        return addresses;
                    })
                    .abortIfNull()
                    .syncLast(addresses -> {
                        final List<Text> contents = new ArrayList<>();

                        addresses.forEach((key, value) -> contents.add(Text.of(TextColors.DARK_GREEN, key, "    ", timeFormatter.format(value))));

                        Sponge.getServiceManager().provide(PaginationService.class).get().builder()
                                .title(Text.of(TextColors.DARK_GREEN, "IP History Associated With ", TextColors.GREEN, optionalUser.get().getName()))
                                .contents(contents)
                                .linesPerPage(14)
                                .padding(Text.of(TextColors.GRAY, "="))
                                .sendTo(src);
                    })
                    .execute();
        } else {
            throw new CommandException(Text.of(TextColors.RED, "You must specify either an IP address or a player."));
        }

        return CommandResult.success();
    }

}
